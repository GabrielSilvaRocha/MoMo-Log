from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.running import RunningActivity
from app.models.training import StrengthSetLog, StrengthWorkoutExercise, TrainingSession
from app.schemas.training import TrainingSessionRead

router = APIRouter(prefix="/history", tags=["history"])


def _session_options():
    return (
        selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs),
        selectinload(TrainingSession.strength_exercises).joinedload(StrengthWorkoutExercise.exercise),
        selectinload(TrainingSession.running_activity),
    )


@router.get("/sessions", response_model=list[TrainingSessionRead])
def list_session_history(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    session_type: str | None = None,
    status: str | None = None,
    limit: int = 50,
    db: Session = Depends(get_db),
) -> list[TrainingSession]:
    query = select(TrainingSession).options(*_session_options()).where(TrainingSession.user_id == user_id)

    if date_from is not None:
        query = query.where(TrainingSession.scheduled_date >= date_from)
    if date_to is not None:
        query = query.where(TrainingSession.scheduled_date <= date_to)
    if session_type:
        query = query.where(TrainingSession.session_type == session_type)
    if status:
        query = query.where(TrainingSession.status == status)

    safe_limit = min(max(limit, 1), 200)
    result = db.execute(query.order_by(TrainingSession.scheduled_date.desc(), TrainingSession.id.desc()).limit(safe_limit))
    return list(result.scalars().all())


@router.get("/summary")
def get_history_summary(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    db: Session = Depends(get_db),
) -> dict:
    today = date.today()
    start = date_from or (today - timedelta(days=30))
    end = date_to or today

    sessions = list(
        db.execute(
            select(TrainingSession)
            .options(selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs))
            .where(
                TrainingSession.user_id == user_id,
                TrainingSession.scheduled_date >= start,
                TrainingSession.scheduled_date <= end,
            )
        ).scalars().all()
    )

    completed_sessions = [session for session in sessions if session.status == "completed"]
    adapted_sessions = [session for session in sessions if session.status == "adapted"]
    skipped_sessions = [session for session in sessions if session.status == "skipped"]
    strength_sessions = [session for session in sessions if session.session_type == "strength"]
    running_sessions = [session for session in sessions if session.session_type == "running"]

    strength_volume = Decimal("0")
    total_sets = 0
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                strength_volume += Decimal(set_log.reps) * Decimal(set_log.load)
                total_sets += 1

    start_dt = datetime.combine(start, time.min, tzinfo=timezone.utc)
    end_dt = datetime.combine(end + timedelta(days=1), time.min, tzinfo=timezone.utc)
    running_activities = list(
        db.execute(
            select(RunningActivity).where(
                RunningActivity.user_id == user_id,
                RunningActivity.start_date >= start_dt,
                RunningActivity.start_date < end_dt,
            )
        ).scalars().all()
    )

    running_distance_km = sum((activity.distance_m for activity in running_activities), Decimal("0")) / Decimal("1000")
    running_time_s = sum((activity.moving_time_s for activity in running_activities), 0)
    average_pace = None
    if running_distance_km > 0:
        average_pace = Decimal(running_time_s) / Decimal("60") / running_distance_km

    trainable_sessions = [session for session in sessions if session.session_type != "rest"]
    completion_rate = 0.0
    if trainable_sessions:
        completion_rate = round((len(completed_sessions) / len(trainable_sessions)) * 100, 2)

    return {
        "user_id": user_id,
        "date_from": start,
        "date_to": end,
        "total_sessions": len(sessions),
        "completed_sessions": len(completed_sessions),
        "adapted_sessions": len(adapted_sessions),
        "skipped_sessions": len(skipped_sessions),
        "strength_sessions": len(strength_sessions),
        "running_sessions": len(running_sessions),
        "total_sets": total_sets,
        "strength_volume": strength_volume,
        "running_activities": len(running_activities),
        "running_distance_km": running_distance_km,
        "running_time_s": running_time_s,
        "average_pace": average_pace,
        "completion_rate": completion_rate,
    }
