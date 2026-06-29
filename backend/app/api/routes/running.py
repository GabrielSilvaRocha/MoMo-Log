from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.running import RunningActivity, StravaAccount, StravaSyncLog
from app.models.training import TrainingSession
from app.models.user import User
from app.schemas.running import (
    RunningActivityCreate,
    RunningActivityRead,
    StravaStatusRead,
    StravaSyncResponse,
)

router = APIRouter(tags=["running"])


def _calculate_average_speed(distance_m: Decimal, moving_time_s: int) -> Decimal:
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
            session.finished_at = payload.start_date

    db.commit()
    db.refresh(activity)
    return activity


@router.get("/strava/status", response_model=StravaStatusRead)
def get_strava_status(user_id: int, db: Session = Depends(get_db)) -> dict:
    account = db.execute(select(StravaAccount).where(StravaAccount.user_id == user_id)).scalar_one_or_none()
    return {
        "user_id": user_id,
        "connected": account is not None,
        "strava_athlete_id": account.strava_athlete_id if account else None,
        "token_expires_at": account.token_expires_at if account else None,
    }


@router.post("/strava/sync", response_model=StravaSyncResponse)
def sync_strava_mock(user_id: int, db: Session = Depends(get_db)) -> dict:
    """Sincronização simulada para a fase local do projeto.

    A integração OAuth real será implementada depois. Nesta release, o endpoint
    cria atividades de demonstração para sessões de corrida planejadas sem
    atividade vinculada, preservando o contrato da API.
    """
    if db.get(User, user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    account = db.execute(select(StravaAccount).where(StravaAccount.user_id == user_id)).scalar_one_or_none()
    if account is None:
        account = StravaAccount(
            user_id=user_id,
            strava_athlete_id=f"demo-athlete-{user_id}",
            access_token_encrypted=None,
            refresh_token_encrypted=None,
            token_expires_at=None,
        )
        db.add(account)
        db.flush()

    running_sessions = list(
        db.execute(
            select(TrainingSession)
            .where(TrainingSession.user_id == user_id, TrainingSession.session_type == "running")
            .order_by(TrainingSession.scheduled_date, TrainingSession.id)
        ).scalars().all()
    )

    imported = 0
    ignored = 0

    for session in running_sessions:
        existing = db.execute(
            select(RunningActivity).where(RunningActivity.training_session_id == session.id)
        ).scalar_one_or_none()
        if existing is not None:
            ignored += 1
            continue

        distance_m = Decimal("5000.00") if "Rodagem" in session.title else Decimal("6000.00")
        moving_time_s = 1800 if distance_m == Decimal("5000.00") else 2100
        start_datetime = datetime.combine(session.scheduled_date, datetime.min.time(), tzinfo=timezone.utc)

        activity = RunningActivity(
            user_id=user_id,
            training_session_id=session.id,
            strava_account_id=account.id,
            strava_activity_id=f"demo-strava-{session.id}",
            name=session.title,
            distance_m=distance_m,
            moving_time_s=moving_time_s,
            elapsed_time_s=moving_time_s + 60,
            average_speed=_calculate_average_speed(distance_m, moving_time_s),
            average_pace=_calculate_average_pace(distance_m, moving_time_s),
            max_speed=Decimal("4.20"),
            total_elevation_gain=Decimal("35.00"),
            activity_type="Run",
            source="strava_mock",
            start_date=start_datetime,
        )
        session.status = "completed"
        session.started_at = start_datetime
        session.finished_at = start_datetime
        db.add(activity)
        imported += 1

    sync_log = StravaSyncLog(
        user_id=user_id,
        finished_at=datetime.now(timezone.utc),
        imported_count=imported,
        updated_count=0,
        ignored_count=ignored,
        status="success",
        message="Sincronização simulada concluída. OAuth real será implementado em release futura.",
    )
    db.add(sync_log)
    db.commit()

    return {
        "imported": imported,
        "updated": 0,
        "ignored": ignored,
        "status": "success",
        "message": "Sincronização simulada concluída.",
    }
