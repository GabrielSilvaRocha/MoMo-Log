from __future__ import annotations

import csv
from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal
from io import StringIO

from fastapi import APIRouter, Depends, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.running import RunningActivity
from app.models.training import StrengthWorkoutExercise, TrainingSession

router = APIRouter(prefix="/reports", tags=["reports"])


def _period(date_from: date | None, date_to: date | None) -> tuple[date, date]:
    today = date.today()
    start = date_from or (today - timedelta(days=30))
    end = date_to or today
    return start, end


def _utc_bounds(start: date, end: date) -> tuple[datetime, datetime]:
    start_dt = datetime.combine(start, time.min, tzinfo=timezone.utc)
    end_dt = datetime.combine(end + timedelta(days=1), time.min, tzinfo=timezone.utc)
    return start_dt, end_dt


def _csv_response(filename: str, rows: list[dict[str, object]], fieldnames: list[str]) -> Response:
    output = StringIO()
    writer = csv.DictWriter(output, fieldnames=fieldnames, extrasaction="ignore")
    writer.writeheader()
    writer.writerows(rows)

    return Response(
        content=output.getvalue(),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@router.get("/overview")
def get_report_overview(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    db: Session = Depends(get_db),
) -> dict:
    start, end = _period(date_from, date_to)
    start_dt, end_dt = _utc_bounds(start, end)

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

    running_activities = list(
        db.execute(
            select(RunningActivity).where(
                RunningActivity.user_id == user_id,
                RunningActivity.start_date >= start_dt,
                RunningActivity.start_date < end_dt,
            )
        ).scalars().all()
    )

    completed_sessions = [session for session in sessions if session.status == "completed"]
    adapted_sessions = [session for session in sessions if session.status == "adapted"]
    skipped_sessions = [session for session in sessions if session.status == "skipped"]
    strength_sessions = [session for session in sessions if session.session_type == "strength"]
    running_sessions = [session for session in sessions if session.session_type == "running"]
    trainable_sessions = [session for session in sessions if session.session_type != "rest"]

    strength_volume = Decimal("0")
    total_sets = 0
    total_rpe = 0
    rpe_count = 0
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                strength_volume += Decimal(set_log.reps) * Decimal(set_log.load)
                total_sets += 1
                if set_log.rpe is not None:
                    total_rpe += int(set_log.rpe)
                    rpe_count += 1

    running_distance_km = sum((activity.distance_m for activity in running_activities), Decimal("0")) / Decimal("1000")
    running_time_s = sum((activity.moving_time_s for activity in running_activities), 0)
    treadmill_runs = [activity for activity in running_activities if activity.source == "manual_treadmill"]

    average_pace = None
    if running_distance_km > 0:
        average_pace = Decimal(running_time_s) / Decimal("60") / running_distance_km

    completion_rate = 0.0
    if trainable_sessions:
        completion_rate = round((len(completed_sessions) / len(trainable_sessions)) * 100, 2)

    average_rpe = None
    if rpe_count:
        average_rpe = round(total_rpe / rpe_count, 2)

    insights: list[str] = []
    if running_distance_km > 0:
        insights.append(f"Você acumulou {running_distance_km:.1f} km de corrida no período.")
    if strength_volume > 0:
        insights.append(f"Volume registrado de musculação: {strength_volume:.0f} kg.")
    if treadmill_runs:
        insights.append(f"{len(treadmill_runs)} corrida(s) foram registradas na esteira.")
    if completion_rate >= 80:
        insights.append("Consistência alta no período analisado.")
    elif trainable_sessions:
        insights.append("Há espaço para melhorar a consistência dos treinos planejados.")
    if adapted_sessions:
        insights.append("Você adaptou treinos no período, mantendo o plano flexível.")

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
        "strength_volume": strength_volume,
        "total_sets": total_sets,
        "average_rpe": average_rpe,
        "running_activities": len(running_activities),
        "treadmill_runs": len(treadmill_runs),
        "running_distance_km": running_distance_km,
        "running_time_s": running_time_s,
        "average_pace": average_pace,
        "completion_rate": completion_rate,
        "insights": insights,
    }


@router.get("/export/sessions.csv")
def export_sessions_csv(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    db: Session = Depends(get_db),
) -> Response:
    start, end = _period(date_from, date_to)
    sessions = list(
        db.execute(
            select(TrainingSession).where(
                TrainingSession.user_id == user_id,
                TrainingSession.scheduled_date >= start,
                TrainingSession.scheduled_date <= end,
            ).order_by(TrainingSession.scheduled_date.asc(), TrainingSession.id.asc())
        ).scalars().all()
    )

    rows = [
        {
            "id": session.id,
            "scheduled_date": session.scheduled_date,
            "session_type": session.session_type,
            "title": session.title,
            "status": session.status,
            "source": session.source,
            "started_at": session.started_at or "",
            "finished_at": session.finished_at or "",
            "notes": session.notes or "",
        }
        for session in sessions
    ]
    return _csv_response(
        "mo2log_sessions.csv",
        rows,
        ["id", "scheduled_date", "session_type", "title", "status", "source", "started_at", "finished_at", "notes"],
    )


@router.get("/export/running.csv")
def export_running_csv(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    db: Session = Depends(get_db),
) -> Response:
    start, end = _period(date_from, date_to)
    start_dt, end_dt = _utc_bounds(start, end)
    activities = list(
        db.execute(
            select(RunningActivity).where(
                RunningActivity.user_id == user_id,
                RunningActivity.start_date >= start_dt,
                RunningActivity.start_date < end_dt,
            ).order_by(RunningActivity.start_date.asc(), RunningActivity.id.asc())
        ).scalars().all()
    )

    rows = [
        {
            "id": activity.id,
            "start_date": activity.start_date,
            "name": activity.name,
            "source": activity.source,
            "activity_type": activity.activity_type,
            "distance_km": Decimal(activity.distance_m) / Decimal("1000"),
            "moving_time_s": activity.moving_time_s,
            "elapsed_time_s": activity.elapsed_time_s,
            "average_pace_min_km": activity.average_pace or "",
            "average_speed_m_s": activity.average_speed or "",
            "total_elevation_gain": activity.total_elevation_gain or "",
        }
        for activity in activities
    ]
    return _csv_response(
        "mo2log_running.csv",
        rows,
        [
            "id",
            "start_date",
            "name",
            "source",
            "activity_type",
            "distance_km",
            "moving_time_s",
            "elapsed_time_s",
            "average_pace_min_km",
            "average_speed_m_s",
            "total_elevation_gain",
        ],
    )


@router.get("/export/strength.csv")
def export_strength_csv(
    user_id: int,
    date_from: date | None = None,
    date_to: date | None = None,
    db: Session = Depends(get_db),
) -> Response:
    start, end = _period(date_from, date_to)
    sessions = list(
        db.execute(
            select(TrainingSession)
            .options(
                selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs),
                selectinload(TrainingSession.strength_exercises).joinedload(StrengthWorkoutExercise.exercise),
            )
            .where(
                TrainingSession.user_id == user_id,
                TrainingSession.scheduled_date >= start,
                TrainingSession.scheduled_date <= end,
                TrainingSession.session_type == "strength",
            )
            .order_by(TrainingSession.scheduled_date.asc(), TrainingSession.id.asc())
        ).scalars().all()
    )

    rows: list[dict[str, object]] = []
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            exercise_name = workout_exercise.exercise.name if workout_exercise.exercise else ""
            for set_log in workout_exercise.set_logs:
                rows.append(
                    {
                        "session_id": session.id,
                        "scheduled_date": session.scheduled_date,
                        "session_title": session.title,
                        "exercise": exercise_name,
                        "set_number": set_log.set_number,
                        "reps": set_log.reps,
                        "load": set_log.load,
                        "volume": Decimal(set_log.reps) * Decimal(set_log.load),
                        "rir": set_log.rir if set_log.rir is not None else "",
                        "rpe": set_log.rpe if set_log.rpe is not None else "",
                        "completed_at": set_log.completed_at,
                    }
                )

    return _csv_response(
        "mo2log_strength.csv",
        rows,
        [
            "session_id",
            "scheduled_date",
            "session_title",
            "exercise",
            "set_number",
            "reps",
            "load",
            "volume",
            "rir",
            "rpe",
            "completed_at",
        ],
    )
