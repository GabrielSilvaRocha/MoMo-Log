from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP

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
    StrengthLoadProgressionRead,
    StrengthSetLogRead,
    StrengthWorkoutExerciseCreate,
    StrengthWorkoutExerciseRead,
    TrainingPlanRead,
    TrainingSessionCreate,
    TrainingSessionRead,
    TrainingSessionReschedule,
    TrainingSessionUpdate,
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
        selectinload(TrainingSession.running_activity),
    )


def _round_load_step(value: Decimal, step: Decimal = Decimal("2.50")) -> Decimal:
    if value <= 0:
        return Decimal("0.00")
    return ((value / step).to_integral_value(rounding=ROUND_HALF_UP) * step).quantize(Decimal("0.01"))


def _average_decimal(values: list[Decimal]) -> Decimal | None:
    if not values:
        return None
    return (sum(values, Decimal("0.00")) / Decimal(len(values))).quantize(Decimal("0.01"))


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


@router.patch("/training-sessions/{session_id}", response_model=TrainingSessionRead)
def update_training_session(
    session_id: int,
    payload: TrainingSessionUpdate,
    db: Session = Depends(get_db),
) -> TrainingSession:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    data = payload.model_dump(exclude_unset=True)
    for field, value in data.items():
        setattr(session, field, value)

    db.commit()
    return get_training_session(session_id, db)


@router.delete("/training-sessions/{session_id}", status_code=204)
def delete_training_session(session_id: int, db: Session = Depends(get_db)) -> None:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    db.delete(session)
    db.commit()


@router.post(
    "/training-sessions/{session_id}/strength-exercises",
    response_model=StrengthWorkoutExerciseRead,
    status_code=201,
)
def add_strength_exercise_to_session(
    session_id: int,
    payload: StrengthWorkoutExerciseCreate,
    db: Session = Depends(get_db),
) -> StrengthWorkoutExercise:
    session = db.get(TrainingSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Training session not found")

    if session.session_type != "strength":
        raise HTTPException(status_code=400, detail="Strength exercises can only be added to strength sessions")

    if db.get(Exercise, payload.exercise_id) is None:
        raise HTTPException(status_code=404, detail="Exercise not found")

    order_index = payload.order_index
    if order_index is None:
        current_max = db.execute(
            select(func.max(StrengthWorkoutExercise.order_index)).where(
                StrengthWorkoutExercise.training_session_id == session_id
            )
        ).scalar_one()
        order_index = (current_max or 0) + 1

    workout_exercise = StrengthWorkoutExercise(
        training_session_id=session_id,
        exercise_id=payload.exercise_id,
        order_index=order_index,
        planned_sets=payload.planned_sets,
        planned_reps=payload.planned_reps,
        planned_load=payload.planned_load,
        rest_seconds=payload.rest_seconds,
        notes=payload.notes,
    )
    db.add(workout_exercise)
    db.commit()
    db.refresh(workout_exercise)
    return workout_exercise


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



@router.get("/strength/exercises/{exercise_id}/load-progression", response_model=StrengthLoadProgressionRead)
def get_strength_load_progression(
    exercise_id: int,
    user_id: int,
    db: Session = Depends(get_db),
) -> dict:
    if db.get(User, user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")
    if db.get(Exercise, exercise_id) is None:
        raise HTTPException(status_code=404, detail="Exercise not found")

    rows = db.execute(
        select(StrengthSetLog)
        .join(StrengthWorkoutExercise, StrengthSetLog.strength_workout_exercise_id == StrengthWorkoutExercise.id)
        .join(TrainingSession, StrengthWorkoutExercise.training_session_id == TrainingSession.id)
        .where(TrainingSession.user_id == user_id, StrengthWorkoutExercise.exercise_id == exercise_id)
        .order_by(StrengthSetLog.completed_at.desc(), StrengthSetLog.id.desc())
        .limit(8)
    ).scalars().all()

    if not rows:
        return {
            "user_id": user_id,
            "exercise_id": exercise_id,
            "sample_sets": 0,
            "recommendation": "start",
            "rationale": "Sem histórico registrado para este exercício. Use a carga planejada ou uma carga confortável para calibrar.",
        }

    latest = rows[0]
    loads = [Decimal(item.load) for item in rows]
    rirs = [Decimal(item.rir) for item in rows if item.rir is not None]
    rpes = [Decimal(item.rpe) for item in rows if item.rpe is not None]
    average_rir = _average_decimal(rirs)
    average_rpe = _average_decimal(rpes)
    average_load = _average_decimal(loads)
    best_recent_load = max(loads)
    latest_load = Decimal(latest.load)
    increment = Decimal("2.50")

    recommendation = "maintain"
    suggested_load = latest_load
    rationale = "Mantenha a carga atual até registrar mais séries com boa execução."

    if latest_load <= 0:
        rationale = "Exercício sem carga externa registrada. Mantenha o controle por reps, tempo ou qualidade de execução."
    elif average_rir is not None and average_rir >= Decimal("2.00") and (average_rpe is None or average_rpe <= Decimal("8.00")):
        recommendation = "increase"
        suggested_load = _round_load_step(latest_load + increment)
        rationale = "Histórico recente mostra margem de repetição e esforço controlado. Pequeno aumento de carga é recomendado."
    elif (average_rir is not None and average_rir <= Decimal("0.50")) or (average_rpe is not None and average_rpe >= Decimal("9.00")):
        recommendation = "reduce"
        suggested_load = _round_load_step(max(Decimal("0.00"), latest_load - increment))
        rationale = "Esforço recente ficou muito alto. Reduza levemente ou mantenha até estabilizar a execução."

    return {
        "user_id": user_id,
        "exercise_id": exercise_id,
        "sample_sets": len(rows),
        "latest_load": latest_load,
        "latest_reps": latest.reps,
        "latest_rir": latest.rir,
        "latest_rpe": latest.rpe,
        "average_load": average_load,
        "best_recent_load": best_recent_load,
        "suggested_load": suggested_load,
        "recommendation": recommendation,
        "rationale": rationale,
    }


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
