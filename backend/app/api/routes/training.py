from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload, selectinload

from app.database.dependencies import get_db
from app.models.exercise import Exercise, ExerciseAlternative
from app.models.training import (
    ExerciseSwapLog,
    StrengthSetLog,
    StrengthWorkoutExercise,
    TrainingPlan,
    TrainingSession,
)
from app.models.user import User
from app.schemas.training import (
    ExerciseSwapCreate,
    ExerciseSwapRead,
    StrengthSetLogCreate,
    StrengthSetLogRead,
    TrainingPlanRead,
    TrainingSessionCreate,
    TrainingSessionRead,
    TrainingSessionReschedule,
)

router = APIRouter(tags=["training"])


def _week_bounds(reference_date: date | None = None) -> tuple[date, date]:
    today = reference_date or date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)
    return week_start, week_end


def _session_query():
    return select(TrainingSession).options(
        selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs),
        selectinload(TrainingSession.strength_exercises).joinedload(StrengthWorkoutExercise.exercise),
    )


@router.get("/training-plans/current", response_model=TrainingPlanRead)
def get_current_training_plan(user_id: int, db: Session = Depends(get_db)) -> TrainingPlan:
    plan = db.execute(
        select(TrainingPlan)
        .where(TrainingPlan.user_id == user_id, TrainingPlan.status == "active")
        .order_by(TrainingPlan.start_date.desc())
    ).scalar_one_or_none()

    if plan is None:
        raise HTTPException(status_code=404, detail="Current training plan not found")

    return plan


@router.get("/training-sessions/week", response_model=list[TrainingSessionRead])
def list_week_sessions(
    user_id: int,
    reference_date: date | None = None,
    db: Session = Depends(get_db),
) -> list[TrainingSession]:
    week_start, week_end = _week_bounds(reference_date)
    result = db.execute(
        _session_query()
        .where(
            TrainingSession.user_id == user_id,
            TrainingSession.scheduled_date >= week_start,
            TrainingSession.scheduled_date <= week_end,
        )
        .order_by(TrainingSession.scheduled_date, TrainingSession.id)
    )
    return list(result.scalars().all())


@router.post("/training-sessions", response_model=TrainingSessionRead, status_code=201)
def create_training_session(
    payload: TrainingSessionCreate,
    db: Session = Depends(get_db),
) -> TrainingSession:
    if db.get(User, payload.user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    if payload.training_plan_id is not None and db.get(TrainingPlan, payload.training_plan_id) is None:
        raise HTTPException(status_code=404, detail="Training plan not found")

    session = TrainingSession(**payload.model_dump(), status="planned")
    db.add(session)
    db.commit()
    db.refresh(session)
    return session


@router.get("/training-sessions/{session_id}", response_model=TrainingSessionRead)
def get_training_session(session_id: int, db: Session = Depends(get_db)) -> TrainingSession:
    session = db.execute(_session_query().where(TrainingSession.id == session_id)).scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")
    return session


@router.post("/training-sessions/{session_id}/start", response_model=TrainingSessionRead)
def start_training_session(session_id: int, db: Session = Depends(get_db)) -> TrainingSession:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    if session.status == "completed":
        raise HTTPException(status_code=400, detail="Completed sessions cannot be started")

    session.status = "in_progress"
    session.started_at = datetime.now(timezone.utc)
    db.commit()
    return get_training_session(session_id, db)


@router.post("/training-sessions/{session_id}/finish", response_model=TrainingSessionRead)
def finish_training_session(session_id: int, db: Session = Depends(get_db)) -> TrainingSession:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    session.status = "completed"
    session.finished_at = datetime.now(timezone.utc)
    if session.started_at is None:
        session.started_at = session.finished_at

    db.commit()
    return get_training_session(session_id, db)


@router.post("/training-sessions/{session_id}/reschedule", response_model=TrainingSessionRead)
def reschedule_training_session(
    session_id: int,
    payload: TrainingSessionReschedule,
    db: Session = Depends(get_db),
) -> TrainingSession:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    session.scheduled_date = payload.new_date
    session.status = "rescheduled"
    session.notes = payload.reason or session.notes
    db.commit()
    return get_training_session(session_id, db)


@router.post("/strength/set-logs", response_model=StrengthSetLogRead, status_code=201)
def create_strength_set_log(
    payload: StrengthSetLogCreate,
    db: Session = Depends(get_db),
) -> StrengthSetLog:
    workout_exercise = db.get(StrengthWorkoutExercise, payload.strength_workout_exercise_id)
    if workout_exercise is None:
        raise HTTPException(status_code=404, detail="Strength workout exercise not found")

    existing_set = db.execute(
        select(StrengthSetLog).where(
            StrengthSetLog.strength_workout_exercise_id == payload.strength_workout_exercise_id,
            StrengthSetLog.set_number == payload.set_number,
        )
    ).scalar_one_or_none()

    if existing_set is not None:
        existing_set.reps = payload.reps
        existing_set.load = payload.load
        existing_set.rir = payload.rir
        existing_set.rpe = payload.rpe
        db.commit()
        db.refresh(existing_set)
        return existing_set

    set_log = StrengthSetLog(**payload.model_dump())
    db.add(set_log)
    db.commit()
    db.refresh(set_log)
    return set_log


@router.post("/training-sessions/{session_id}/swap-exercise", response_model=ExerciseSwapRead)
def swap_exercise(
    session_id: int,
    payload: ExerciseSwapCreate,
    db: Session = Depends(get_db),
) -> dict:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    workout_exercise = db.get(StrengthWorkoutExercise, payload.strength_workout_exercise_id)
    if workout_exercise is None or workout_exercise.training_session_id != session_id:
        raise HTTPException(status_code=404, detail="Workout exercise not found in this session")

    if db.get(Exercise, payload.original_exercise_id) is None:
        raise HTTPException(status_code=404, detail="Original exercise not found")

    if db.get(Exercise, payload.new_exercise_id) is None:
        raise HTTPException(status_code=404, detail="New exercise not found")

    equivalence = db.execute(
        select(ExerciseAlternative).where(
            ExerciseAlternative.exercise_id == payload.original_exercise_id,
            ExerciseAlternative.alternative_exercise_id == payload.new_exercise_id,
        )
    ).scalar_one_or_none()

    equivalence_score = equivalence.equivalence_score if equivalence else None

    workout_exercise.exercise_id = payload.new_exercise_id
    session.status = "adapted" if session.status == "planned" else session.status

    db.add(
        ExerciseSwapLog(
            training_session_id=session_id,
            original_exercise_id=payload.original_exercise_id,
            new_exercise_id=payload.new_exercise_id,
            reason=payload.reason,
            equivalence_score=equivalence_score,
        )
    )
    db.commit()

    return {
        "status": "swapped",
        "equivalence_score": equivalence_score,
        "message": "Exercício substituído e registrado no histórico da sessão.",
    }
