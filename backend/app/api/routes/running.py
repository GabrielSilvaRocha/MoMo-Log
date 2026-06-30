from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.running import RunningActivity
from app.models.training import TrainingSession
from app.models.user import User
from app.schemas.running import RunningActivityCreate, RunningActivityRead

router = APIRouter(tags=["running"])


def _calculate_average_speed(distance_m: Decimal, moving_time_s: int) -> Decimal:
    # m/s para manter compatibilidade com o campo legado.
    return (distance_m / Decimal(moving_time_s)).quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)


def _calculate_average_pace(distance_m: Decimal, moving_time_s: int) -> Decimal:
    minutes = Decimal(moving_time_s) / Decimal(60)
    kilometers = distance_m / Decimal(1000)
    return (minutes / kilometers).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


@router.get("/running-activities", response_model=list[RunningActivityRead])
def list_running_activities(user_id: int | None = None, db: Session = Depends(get_db)) -> list[RunningActivity]:
    query = select(RunningActivity).order_by(RunningActivity.start_date.desc())
    if user_id is not None:
        query = query.where(RunningActivity.user_id == user_id)
    return list(db.execute(query).scalars().all())


@router.get("/running-activities/{activity_id}", response_model=RunningActivityRead)
def get_running_activity(activity_id: int, db: Session = Depends(get_db)) -> RunningActivity:
    activity = db.get(RunningActivity, activity_id)
    if activity is None:
        raise HTTPException(status_code=404, detail="Running activity not found")
    return activity


@router.post("/running-activities", response_model=RunningActivityRead, status_code=201)
def create_running_activity(payload: RunningActivityCreate, db: Session = Depends(get_db)) -> RunningActivity:
    """Mantém cadastro manual de corrida para histórico, principalmente esteira.

    O módulo de corrida deixou de depender de integrações externas. Use este
    endpoint para registrar atividades manuais realizadas fora do executor guiado.
    """
    if db.get(User, payload.user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    if payload.training_session_id is not None:
        session = db.get(TrainingSession, payload.training_session_id)
        if session is None:
            raise HTTPException(status_code=404, detail="Training session not found")
        if session.user_id != payload.user_id:
            raise HTTPException(status_code=400, detail="Training session does not belong to this user")
        if session.session_type != "running":
            raise HTTPException(status_code=400, detail="Training session is not a running session")

    data = payload.model_dump()
    data["strava_account_id"] = None
    data["strava_activity_id"] = None
    data["source"] = data.get("source") or "manual_treadmill"
    if data.get("average_speed") is None:
        data["average_speed"] = _calculate_average_speed(payload.distance_m, payload.moving_time_s)
    if data.get("average_pace") is None:
        data["average_pace"] = _calculate_average_pace(payload.distance_m, payload.moving_time_s)

    activity = RunningActivity(**data)
    db.add(activity)

    if payload.training_session_id is not None:
        session = db.get(TrainingSession, payload.training_session_id)
        if session is not None:
            session.status = "completed"
            session.started_at = payload.start_date
            session.finished_at = datetime.now(timezone.utc)

    db.commit()
    db.refresh(activity)
    return activity
