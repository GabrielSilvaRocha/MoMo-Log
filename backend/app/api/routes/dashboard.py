from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.running import RunningActivity
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
                selectinload(TrainingSession.running_activity),
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
    strength_sessions = [session for session in sessions if session.session_type == "strength"]
    running_sessions = [session for session in sessions if session.session_type == "running"]
    rest_sessions = [session for session in sessions if session.session_type == "rest"]
    completed_strength_sessions = [session for session in strength_sessions if session.status == "completed"]
    completed_running_sessions = [session for session in running_sessions if session.status == "completed"]

    weekly_strength_volume = Decimal("0")
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                weekly_strength_volume += Decimal(set_log.reps) * Decimal(set_log.load)

    trainable_sessions = [session for session in sessions if session.session_type != "rest"]
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
        ).scalars().all()
    )
    weekly_running_distance_km = sum(
        (activity.distance_m for activity in running_activities), Decimal("0")
    ) / Decimal("1000")
    planned_strength = len(strength_sessions)
    planned_running = len(running_sessions)
    hybrid_bonus = 0
    if completed_strength_sessions:
        hybrid_bonus += 20
    if weekly_running_distance_km > 0 or completed_running_sessions:
        hybrid_bonus += 20
    volume_bonus = min(float(weekly_strength_volume / Decimal("1000")), 30.0)
    running_bonus = min(float(weekly_running_distance_km) * 2, 30.0)
    hybrid_score = round(min(100.0, completion_rate * 0.4 + hybrid_bonus + volume_bonus + running_bonus), 2)
    if completion_rate < 60:
        next_focus = "Fechar as sessões planejadas restantes da semana."
    elif not completed_strength_sessions:
        next_focus = "Adicionar estímulo de força para manter evolução híbrida."
    elif weekly_running_distance_km <= 0 and not completed_running_sessions:
        next_focus = "Registrar corrida ou executar o próximo treino do Running Coach."
    else:
        next_focus = "Manter consistência e revisar carga/pace na próxima sessão."
    recovery_balance = "adequate" if rest_sessions else "watch"
    training_mix = [
        {
            "key": "strength",
            "label": "Força",
            "planned": planned_strength,
            "completed": len(completed_strength_sessions),
        },
        {
            "key": "running",
            "label": "Corrida",
            "planned": planned_running,
            "completed": len(completed_running_sessions),
        },
        {
            "key": "recovery",
            "label": "Recuperação",
            "planned": len(rest_sessions),
            "completed": len([session for session in rest_sessions if session.status == "completed"]),
        },
    ]

    return {
        "user_id": user_id,
        "completed_sessions": completed_sessions,
        "today_sessions": today_sessions,
        "upcoming_sessions": upcoming_sessions,
        "weekly_strength_volume": weekly_strength_volume,
        "weekly_running_distance_km": weekly_running_distance_km,
        "completion_rate": completion_rate,
        "weekly_strength_sessions": planned_strength,
        "weekly_running_sessions": planned_running,
        "hybrid_score": hybrid_score,
        "next_focus": next_focus,
        "recovery_balance": recovery_balance,
        "training_mix": training_mix,
    }
