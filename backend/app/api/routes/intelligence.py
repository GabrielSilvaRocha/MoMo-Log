from __future__ import annotations

from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP
from typing import Any

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.running import RunningActivity
from app.models.training import StrengthWorkoutExercise, TrainingSession

router = APIRouter(prefix="/intelligence", tags=["intelligence"])


def _week_bounds(reference_date: date | None = None) -> tuple[date, date]:
    today = reference_date or date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)
    return week_start, week_end


def _date_range_to_datetimes(start: date, end: date) -> tuple[datetime, datetime]:
    start_dt = datetime.combine(start, time.min, tzinfo=timezone.utc)
    end_dt = datetime.combine(end + timedelta(days=1), time.min, tzinfo=timezone.utc)
    return start_dt, end_dt


def _decimal_to_float(value: Decimal | int | float | None) -> float:
    if value is None:
        return 0.0
    return float(value)


def _pace_label(minutes_per_km: Decimal | None) -> str | None:
    if minutes_per_km is None:
        return None
    total_seconds = int((minutes_per_km * Decimal(60)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
    minutes, seconds = divmod(total_seconds, 60)
    return f"{minutes}:{seconds:02d}/km"


def _duration_label(seconds: int | None) -> str | None:
    if seconds is None:
        return None
    minutes, sec = divmod(int(seconds), 60)
    hours, minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}h {minutes:02d}min {sec:02d}s"
    return f"{minutes}min {sec:02d}s"


def _load_week_data(user_id: int, reference_date: date | None, db: Session) -> tuple[date, date, list[TrainingSession], list[RunningActivity]]:
    week_start, week_end = _week_bounds(reference_date)
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
    start_dt, end_dt = _date_range_to_datetimes(week_start, week_end)
    running_activities = list(
        db.execute(
            select(RunningActivity)
            .where(
                RunningActivity.user_id == user_id,
                RunningActivity.start_date >= start_dt,
                RunningActivity.start_date < end_dt,
            )
            .order_by(RunningActivity.start_date)
        )
        .scalars()
        .all()
    )
    return week_start, week_end, sessions, running_activities


def _strength_volume(sessions: list[TrainingSession]) -> Decimal:
    volume = Decimal("0")
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                volume += Decimal(set_log.reps) * Decimal(set_log.load)
    return volume


def _average_rpe(sessions: list[TrainingSession]) -> float | None:
    values: list[int] = []
    for session in sessions:
        for workout_exercise in session.strength_exercises:
            for set_log in workout_exercise.set_logs:
                if set_log.rpe is not None:
                    values.append(set_log.rpe)
    if not values:
        return None
    return round(sum(values) / len(values), 2)


def _running_distance_km(runs: list[RunningActivity]) -> Decimal:
    return sum((Decimal(run.distance_m) for run in runs), Decimal("0")) / Decimal("1000")


def _average_pace(runs: list[RunningActivity]) -> Decimal | None:
    distance_km = _running_distance_km(runs)
    if distance_km <= 0:
        return None
    seconds = sum((run.moving_time_s for run in runs), 0)
    return Decimal(seconds) / Decimal("60") / distance_km


def _status_count(sessions: list[TrainingSession], status: str) -> int:
    return len([session for session in sessions if session.status == status])


@router.get("/weekly-insights")
def get_weekly_intelligence(
    user_id: int,
    reference_date: date | None = None,
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    week_start, week_end, sessions, runs = _load_week_data(user_id, reference_date, db)
    trainable_sessions = [session for session in sessions if session.session_type != "rest"]
    completed_sessions = [session for session in sessions if session.status == "completed"]
    adapted_sessions = [session for session in sessions if session.status == "adapted"]
    skipped_sessions = [session for session in sessions if session.status == "skipped"]
    strength_sessions = [session for session in sessions if session.session_type == "strength"]
    running_sessions = [session for session in sessions if session.session_type == "running"]

    completion_rate = 0.0
    if trainable_sessions:
        completion_rate = round((len(completed_sessions) / len(trainable_sessions)) * 100, 2)

    strength_volume = _strength_volume(sessions)
    running_km = _running_distance_km(runs)
    average_pace = _average_pace(runs)
    average_rpe = _average_rpe(sessions)

    consistency_component = min(completion_rate, 100)
    running_component = min(float(running_km) / 20 * 100, 100) if running_km > 0 else 0
    strength_component = min(len([s for s in completed_sessions if s.session_type == "strength"]) / 3 * 100, 100)
    adaptation_penalty = min(len(adapted_sessions) * 5 + len(skipped_sessions) * 10, 25)
    hybrid_score = round((consistency_component * 0.45) + (running_component * 0.25) + (strength_component * 0.30) - adaptation_penalty, 1)
    hybrid_score = max(0, min(hybrid_score, 100))

    insights: list[dict[str, str]] = []
    if completion_rate >= 80:
        insights.append({"type": "consistency", "severity": "positive", "title": "Boa consistência", "message": "Você está executando a maior parte do plano da semana."})
    elif trainable_sessions:
        insights.append({"type": "consistency", "severity": "attention", "title": "Consistência abaixo do ideal", "message": "Priorize concluir os próximos treinos antes de aumentar volume ou intensidade."})

    if running_km >= Decimal("15"):
        insights.append({"type": "running", "severity": "positive", "title": "Volume de corrida sólido", "message": f"Você acumulou {running_km:.1f} km nesta semana."})
    elif running_sessions:
        insights.append({"type": "running", "severity": "info", "title": "Corrida ainda pode evoluir", "message": "Registre as corridas de esteira para melhorar a leitura de pace e volume semanal."})

    if strength_volume > 0:
        insights.append({"type": "strength", "severity": "info", "title": "Volume de musculação registrado", "message": f"Volume acumulado: {strength_volume:.0f} kg."})

    if average_rpe and average_rpe >= 8.5:
        insights.append({"type": "effort", "severity": "attention", "title": "Esforço alto", "message": "O RPE médio está alto; mantenha atenção à recuperação e qualidade técnica."})

    if adapted_sessions:
        insights.append({"type": "adaptation", "severity": "attention", "title": "Treinos adaptados", "message": "Houve adaptação de treino. Revise equipamentos indisponíveis e favoritos na tela Exercícios."})

    if not insights:
        insights.append({"type": "baseline", "severity": "info", "title": "Base criada", "message": "Continue registrando treinos para o Mo² LOG gerar insights melhores."})

    recommendations: list[dict[str, str]] = []
    if running_km < Decimal("20"):
        recommendations.append({"title": "Completar meta de corrida", "action": "Cadastre uma corrida de esteira ou uma rodagem curta para aproximar a semana da meta."})
    if len([s for s in completed_sessions if s.session_type == "strength"]) < 3:
        recommendations.append({"title": "Fechar musculação da semana", "action": "Priorize os treinos de força restantes antes de adicionar sessões extras."})
    if skipped_sessions:
        recommendations.append({"title": "Reorganizar sessões puladas", "action": "Use o Planejamento para reagendar as sessões que ficaram para trás."})
    if not recommendations:
        recommendations.append({"title": "Manter execução", "action": "Mantenha o plano atual e registre cargas, reps e corridas com consistência."})

    return {
        "user_id": user_id,
        "reference_date": reference_date or date.today(),
        "week_start": week_start,
        "week_end": week_end,
        "hybrid_score": hybrid_score,
        "summary": {
            "planned_sessions": len(trainable_sessions),
            "completed_sessions": len(completed_sessions),
            "completion_rate": completion_rate,
            "adapted_sessions": len(adapted_sessions),
            "skipped_sessions": len(skipped_sessions),
            "strength_sessions": len(strength_sessions),
            "running_sessions": len(running_sessions),
            "strength_volume": strength_volume,
            "running_distance_km": running_km,
            "running_activities": len(runs),
            "average_pace": average_pace,
            "average_pace_label": _pace_label(average_pace),
            "average_rpe": average_rpe,
        },
        "insights": insights,
        "recommendations": recommendations,
    }


@router.get("/planned-vs-done")
def get_planned_vs_done(
    user_id: int,
    reference_date: date | None = None,
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    week_start, week_end, sessions, runs = _load_week_data(user_id, reference_date, db)
    by_type: dict[str, dict[str, int]] = {}
    for session_type in ["strength", "running", "mobility", "rest"]:
        filtered = [session for session in sessions if session.session_type == session_type]
        by_type[session_type] = {
            "planned": len(filtered),
            "completed": len([session for session in filtered if session.status == "completed"]),
            "adapted": len([session for session in filtered if session.status == "adapted"]),
            "skipped": len([session for session in filtered if session.status == "skipped"]),
        }

    days: list[dict[str, Any]] = []
    cursor = week_start
    while cursor <= week_end:
        day_sessions = [session for session in sessions if session.scheduled_date == cursor]
        days.append(
            {
                "date": cursor,
                "sessions": [
                    {
                        "id": session.id,
                        "title": session.title,
                        "session_type": session.session_type,
                        "status": session.status,
                    }
                    for session in day_sessions
                ],
            }
        )
        cursor += timedelta(days=1)

    return {
        "user_id": user_id,
        "week_start": week_start,
        "week_end": week_end,
        "by_type": by_type,
        "running_registered_km": _running_distance_km(runs),
        "days": days,
    }



def _running_forecast(user_id: int, target_distance_km: Decimal, db: Session) -> dict[str, Any]:
    safe_target = max(target_distance_km, Decimal("1.00")).quantize(Decimal("0.01"))
    runs = list(
        db.execute(
            select(RunningActivity)
            .where(RunningActivity.user_id == user_id, RunningActivity.distance_m > 0, RunningActivity.moving_time_s > 0)
            .order_by(RunningActivity.start_date.desc())
            .limit(20)
        )
        .scalars()
        .all()
    )

    pace_samples: list[dict[str, Any]] = []
    for run in runs:
        distance_km = Decimal(run.distance_m) / Decimal("1000")
        if distance_km <= 0:
            continue
        pace = Decimal(run.moving_time_s) / Decimal("60") / distance_km
        lower_bound = safe_target * Decimal("0.60")
        upper_bound = safe_target * Decimal("1.80")
        relevance = "high" if lower_bound <= distance_km <= upper_bound else "medium"
        pace_samples.append(
            {
                "activity_id": run.id,
                "name": run.name,
                "source": run.source,
                "distance_km": float(distance_km),
                "pace": pace,
                "pace_label": _pace_label(pace),
                "relevance": relevance,
            }
        )

    if not pace_samples:
        baseline_pace = Decimal("6.00")
        predicted_time_s = int((baseline_pace * safe_target * Decimal("60")).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
        return {
            "user_id": user_id,
            "target_distance_km": safe_target,
            "confidence": "low",
            "predicted_time_s": predicted_time_s,
            "predicted_time_label": _duration_label(predicted_time_s),
            "predicted_pace_label": _pace_label(baseline_pace),
            "based_on_runs": 0,
            "notes": ["Sem corridas suficientes. Usando referência inicial de 6:00/km para a distância-alvo."],
            "samples": [],
        }

    high_relevance = [sample for sample in pace_samples if sample["relevance"] == "high"] or pace_samples
    best_pace = min((sample["pace"] for sample in high_relevance), default=Decimal("6"))
    average_recent_pace = sum((sample["pace"] for sample in high_relevance), Decimal("0")) / Decimal(len(high_relevance))
    fatigue_factor = Decimal("1.00")
    if safe_target > Decimal("10"):
        fatigue_factor += min((safe_target - Decimal("10")) * Decimal("0.012"), Decimal("0.18"))
    elif safe_target < Decimal("5"):
        fatigue_factor -= min((Decimal("5") - safe_target) * Decimal("0.01"), Decimal("0.03"))
    predicted_pace = ((best_pace * Decimal("0.65")) + (average_recent_pace * Decimal("0.35"))) * fatigue_factor
    predicted_time_s = int((predicted_pace * safe_target * Decimal("60")).quantize(Decimal("1"), rounding=ROUND_HALF_UP))

    confidence = "low"
    if len(high_relevance) >= 5:
        confidence = "high"
    elif len(high_relevance) >= 2:
        confidence = "medium"

    notes = [
        "Previsão baseada em pace recente registrado no Mo² LOG.",
        "Distâncias maiores recebem ajuste conservador de fadiga.",
        "Para melhorar a previsão, registre corridas próximas da distância-alvo.",
    ]

    return {
        "user_id": user_id,
        "target_distance_km": safe_target,
        "confidence": confidence,
        "predicted_time_s": predicted_time_s,
        "predicted_time_label": _duration_label(predicted_time_s),
        "predicted_pace_label": _pace_label(predicted_pace),
        "based_on_runs": len(high_relevance),
        "notes": notes,
        "samples": [
            {k: v for k, v in sample.items() if k != "pace"}
            for sample in pace_samples[:8]
        ],
    }


@router.get("/forecast-race")
def get_race_forecast(
    user_id: int,
    target_distance_km: Decimal = Decimal("5.00"),
    db: Session = Depends(get_db),
) -> dict[str, Any]:
    return _running_forecast(user_id, target_distance_km, db)


@router.get("/forecast-5k")
def get_5k_forecast(user_id: int, db: Session = Depends(get_db)) -> dict[str, Any]:
    forecast = _running_forecast(user_id, Decimal("5.00"), db)
    return {
        "user_id": forecast["user_id"],
        "confidence": forecast["confidence"],
        "predicted_5k_time_s": forecast["predicted_time_s"],
        "predicted_5k_time_label": forecast["predicted_time_label"],
        "predicted_pace_label": forecast["predicted_pace_label"],
        "based_on_runs": forecast["based_on_runs"],
        "notes": forecast["notes"],
        "samples": forecast["samples"],
    }
