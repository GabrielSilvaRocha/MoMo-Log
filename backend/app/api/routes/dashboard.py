from datetime import date, timedelta
from decimal import Decimal

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.training import StrengthSetLog, StrengthWorkoutExercise, TrainingSession
from app.schemas.training import WeekDashboardRead

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


def _week_bounds(reference_date: date | None = None) -> tuple[date, date]:
    today = reference_date or date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)
    return week_start, week_end


@router.get("/week", response_model=WeekDashboardRead)
def get_week_dashboard(
    user_id: int,
    reference_date: date | None = None,
    db: Session = Depends(get_db),
) -> dict:
    week_start, week_end = _week_bounds(reference_date)
    today = reference_date or date.today()

    sessions = list(
        db.execute(
            select(TrainingSession)
            .options(
                selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs),
                selectinload(TrainingSession.strength_exercises).joinedload(StrengthWorkoutExercise.exercise),
            )
            .where(
                TrainingSession.user_id == user_id,
                TrainingSession.scheduled_date >= week_start,
                TrainingSession.scheduled_date <= week_end,
            )
            .order_by(TrainingSession.scheduled_date, TrainingSession.id)
        ).scalars().all()
    )

    completed_sessions = [session for session in sessions if session.status == "completed"]
    today_sessions = [session for session in sessions if session.scheduled_date == today]
    upcoming_sessions = [
        session
        for session in sessions
        if session.scheduled_date > today and session.status not in {"completed", "skipped"}
    ]

    weekly_strength_volume = Decimal("0")
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                weekly_strength_volume += Decimal(set_log.reps) * Decimal(set_log.load)

    trainable_sessions = [session for session in sessions if session.session_type != "rest"]
    completion_rate = 0.0
    if trainable_sessions:
        completion_rate = round((len(completed_sessions) / len(trainable_sessions)) * 100, 2)

    return {
        "user_id": user_id,
        "completed_sessions": completed_sessions,
        "today_sessions": today_sessions,
        "upcoming_sessions": upcoming_sessions,
        "weekly_strength_volume": weekly_strength_volume,
        "weekly_running_distance_km": Decimal("0"),
        "completion_rate": completion_rate,
    }
