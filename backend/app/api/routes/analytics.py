from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.analytics import Goal, PersonalRecord
from app.models.running import RunningActivity
from app.models.training import StrengthWorkoutExercise, TrainingSession
from app.schemas.analytics import GoalRead, PersonalRecordRead, WeeklyStatisticsRead

router = APIRouter(tags=["analytics"])


def _week_bounds(reference_date: date | None = None) -> tuple[date, date]:
    today = reference_date or date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)
    return week_start, week_end


def _goal_progress(goal: Goal) -> GoalRead:
    progress_percentage = None
    if goal.target_value and Decimal(goal.target_value) > 0:
        progress_percentage = round((Decimal(goal.current_value) / Decimal(goal.target_value)) * 100, 2)
    return GoalRead.model_validate(goal).model_copy(update={"progress_percentage": float(progress_percentage) if progress_percentage is not None else None})


@router.get("/personal-records", response_model=list[PersonalRecordRead])
def list_personal_records(user_id: int, db: Session = Depends(get_db)) -> list[PersonalRecord]:
    return list(
        db.execute(
            select(PersonalRecord)
            .where(PersonalRecord.user_id == user_id)
            .order_by(PersonalRecord.record_type, PersonalRecord.achieved_at.desc().nullslast())
        )
        .scalars()
        .all()
    )


@router.get("/statistics/week", response_model=WeeklyStatisticsRead)
def get_week_statistics(
    user_id: int,
    reference_date: date | None = None,
    db: Session = Depends(get_db),
) -> dict:
    week_start, week_end = _week_bounds(reference_date)
    today = reference_date or date.today()

    sessions = list(
        db.execute(
            select(TrainingSession)
            .options(selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs))
            .where(
                TrainingSession.user_id == user_id,
                TrainingSession.scheduled_date >= week_start,
                TrainingSession.scheduled_date <= week_end,
            )
            .order_by(TrainingSession.scheduled_date, TrainingSession.id)
        )
        .scalars()
        .all()
    )

    completed_sessions = [session for session in sessions if session.status == "completed"]
    upcoming_sessions = [
        session for session in sessions if session.scheduled_date > today and session.status not in {"completed", "skipped"}
    ]
    adapted_sessions = [session for session in sessions if session.status == "adapted"]
    trainable_sessions = [session for session in sessions if session.session_type != "rest"]

    weekly_strength_volume = Decimal("0")
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                weekly_strength_volume += Decimal(set_log.reps) * Decimal(set_log.load)

    completion_rate = 0.0
    if trainable_sessions:
        completion_rate = round((len(completed_sessions) / len(trainable_sessions)) * 100, 2)

    week_start_dt = datetime.combine(week_start, time.min, tzinfo=timezone.utc)
    week_end_dt = datetime.combine(week_end + timedelta(days=1), time.min, tzinfo=timezone.utc)
    running_activities = list(
        db.execute(
            select(RunningActivity).where(
                RunningActivity.user_id == user_id,
                RunningActivity.start_date >= week_start_dt,
                RunningActivity.start_date < week_end_dt,
            )
        )
        .scalars()
        .all()
    )
    weekly_running_distance_km = sum(
        (Decimal(activity.distance_m) for activity in running_activities), Decimal("0")
    ) / Decimal("1000")

    goals = list(
        db.execute(
            select(Goal)
            .where(Goal.user_id == user_id, Goal.status == "active")
            .order_by(Goal.deadline.asc().nullslast(), Goal.id)
        )
        .scalars()
        .all()
    )
    records = list(
        db.execute(
            select(PersonalRecord)
            .where(PersonalRecord.user_id == user_id)
            .order_by(PersonalRecord.achieved_at.desc().nullslast(), PersonalRecord.id)
            .limit(5)
        )
        .scalars()
        .all()
    )

    insights: list[str] = []
    if completion_rate >= 75:
        insights.append("Você está mantendo boa consistência nesta semana.")
    elif trainable_sessions:
        insights.append("Ainda há espaço para recuperar a consistência semanal.")

    if weekly_running_distance_km > 0:
        insights.append(f"Você já acumulou {weekly_running_distance_km:.1f} km de corrida nesta semana.")

    if weekly_strength_volume > 0:
        insights.append(f"Volume de musculação registrado na semana: {weekly_strength_volume:.0f} kg.")

    if adapted_sessions:
        insights.append("Houve treino adaptado; revise as trocas para entender se algum equipamento está atrapalhando sua rotina.")

    return {
        "user_id": user_id,
        "reference_date": today,
        "week_start": week_start,
        "week_end": week_end,
        "completed_sessions": len(completed_sessions),
        "upcoming_sessions": len(upcoming_sessions),
        "adapted_sessions": len(adapted_sessions),
        "strength_sessions_completed": len(
            [session for session in completed_sessions if session.session_type == "strength"]
        ),
        "running_sessions_completed": len([session for session in completed_sessions if session.session_type == "running"]),
        "weekly_strength_volume": weekly_strength_volume,
        "weekly_running_distance_km": weekly_running_distance_km,
        "completion_rate": completion_rate,
        "active_goals": [_goal_progress(goal) for goal in goals],
        "personal_records": records,
        "insights": insights,
    }
