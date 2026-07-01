from datetime import datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.running import (
    RunningExecutionLog,
    RunningGoal,
    RunningPlanSession,
    RunningSpeedAdjustment,
    RunningStepLog,
    RunningWorkoutStep,
)
from app.models.user import User
from app.schemas.running import (
    RunningExecutionRead,
    RunningGoalCreate,
    RunningGoalRead,
    RunningPlanGenerateResponse,
    RunningPlanSessionRead,
    RunningSpeedAdjustmentRead,
    RunningStepLogRead,
    RunningStepAdvanceRead,
)

router = APIRouter(tags=["running-coach"])


def _speed_from_pace(pace_seconds: int | None) -> Decimal | None:
    if not pace_seconds or pace_seconds <= 0:
        return None
    minutes_per_km = Decimal(pace_seconds) / Decimal(60)
    return (Decimal(60) / minutes_per_km).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def _week_bounds(reference_date: datetime) -> tuple[datetime, datetime]:
    start = reference_date - timedelta(days=reference_date.weekday())
    start = start.replace(hour=0, minute=0, second=0, microsecond=0, tzinfo=timezone.utc)
    return start, start + timedelta(days=7)


def _get_active_goal(db: Session, user_id: int) -> RunningGoal | None:
    return db.execute(
        select(RunningGoal)
        .where(RunningGoal.user_id == user_id, RunningGoal.status == "active")
        .order_by(RunningGoal.race_date.asc())
    ).scalar_one_or_none()


def _ensure_user(db: Session, user_id: int) -> None:
    if db.get(User, user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")


WEEKDAY_OFFSETS = {
    "mon": 0,
    "tue": 1,
    "wed": 2,
    "thu": 3,
    "fri": 4,
    "sat": 5,
    "sun": 6,
}

PROGRESSION_PROFILES = {
    "conservative": {
        "easy_step": Decimal("0.15"),
        "tempo_distance_step": Decimal("0.10"),
        "long_step": Decimal("0.25"),
        "interval_rep_step": 1,
        "max_reps": 6,
        "interval_pace_step": 1,
        "tempo_pace_step": 2,
    },
    "balanced": {
        "easy_step": Decimal("0.25"),
        "tempo_distance_step": Decimal("0.15"),
        "long_step": Decimal("0.35"),
        "interval_rep_step": 1,
        "max_reps": 8,
        "interval_pace_step": 1,
        "tempo_pace_step": 4,
    },
    "aggressive": {
        "easy_step": Decimal("0.35"),
        "tempo_distance_step": Decimal("0.20"),
        "long_step": Decimal("0.50"),
        "interval_rep_step": 2,
        "max_reps": 10,
        "interval_pace_step": 2,
        "tempo_pace_step": 6,
    },
}


def _progression_profile(style: str | None) -> dict:
    return PROGRESSION_PROFILES.get(style or "balanced", PROGRESSION_PROFILES["balanced"])


def _goal_weekday_offsets(goal: RunningGoal) -> list[int]:
    offsets: list[int] = []
    for raw_day in (goal.available_weekdays or "mon,tue,wed,thu,fri").split(","):
        day = raw_day.strip().lower()
        if day in WEEKDAY_OFFSETS and WEEKDAY_OFFSETS[day] not in offsets:
            offsets.append(WEEKDAY_OFFSETS[day])
    return offsets or [0, 2, 4]


def _pick_session_offsets(goal: RunningGoal, preferred_offsets: list[int]) -> list[int]:
    available_offsets = _goal_weekday_offsets(goal)
    fallback_offsets = [day for day in range(7) if day not in available_offsets]
    used: set[int] = set()
    picked: list[int] = []
    for preferred in preferred_offsets:
        candidates = sorted(available_offsets, key=lambda day: (abs(day - preferred), day))
        candidates.extend(sorted(fallback_offsets, key=lambda day: (abs(day - preferred), day)))
        for candidate in candidates:
            if candidate not in used:
                picked.append(candidate)
                used.add(candidate)
                break
    return picked


def _interval_reps(week: int, profile: dict) -> int:
    return min(4 + week * int(profile["interval_rep_step"]), int(profile["max_reps"]))


def _interval_pace(week: int, profile: dict) -> int:
    return max(260, 282 - week * int(profile["interval_pace_step"]))


def _session_blueprint(goal: RunningGoal, week: int, profile: dict) -> list[dict]:
    weekly_sessions = min(max(goal.weekly_sessions or 3, 2), 5)
    reps = _interval_reps(week, profile)
    long_offset = WEEKDAY_OFFSETS.get((goal.long_run_weekday or "fri").lower(), 4)
    easy_distance = (Decimal("4.75") + Decimal(min(week, 6)) * profile["easy_step"]).quantize(Decimal("0.01"))
    tempo_distance = (Decimal("5.00") + Decimal(min(week, 5)) * profile["tempo_distance_step"]).quantize(Decimal("0.01"))
    long_distance = (easy_distance + Decimal("1.25") + Decimal(min(week, 5)) * profile["long_step"]).quantize(Decimal("0.01"))
    interval_distance = (Decimal("1.80") + Decimal(reps) * Decimal("0.40")).quantize(Decimal("0.01"))

    sessions = [
        {
            "preferred_offset": 0,
            "session_type": "easy",
            "title": "Rodagem leve na esteira",
            "distance_km": easy_distance,
            "pace": 335,
            "description": "Base aeróbica controlada.",
        }
    ]
    if weekly_sessions >= 4:
        sessions.append(
            {
                "preferred_offset": 1,
                "session_type": "recovery",
                "title": "Rodagem regenerativa",
                "distance_km": max(Decimal("3.20"), easy_distance - Decimal("1.20")).quantize(Decimal("0.01")),
                "pace": 365,
                "description": "Volume leve para somar consistência sem elevar carga.",
            }
        )
    sessions.append(
        {
            "preferred_offset": 2,
            "session_type": "interval",
            "title": f"Intervalado {reps}x400m",
            "distance_km": interval_distance,
            "pace": _interval_pace(week, profile),
            "description": "Tiros com recuperação ativa.",
        }
    )
    if weekly_sessions >= 3:
        sessions.append(
            {
                "preferred_offset": 4,
                "session_type": "tempo",
                "title": "Tempo run em ritmo controlado",
                "distance_km": tempo_distance,
                "pace": max(300, 325 - week * int(profile["tempo_pace_step"])),
                "description": "Ritmo forte sustentável.",
            }
        )
    if weekly_sessions >= 5:
        sessions.append(
            {
                "preferred_offset": long_offset,
                "session_type": "long",
                "title": "Rodagem longa progressiva",
                "distance_km": long_distance,
                "pace": 350,
                "description": "Maior volume da semana em ritmo confortável.",
            }
        )

    offsets = _pick_session_offsets(goal, [int(session["preferred_offset"]) for session in sessions])
    for session, offset in zip(sessions, offsets):
        session["day_offset"] = offset
    return sorted(sessions, key=lambda session: int(session["day_offset"]))


def _build_default_plan(goal: RunningGoal, db: Session) -> int:
    existing_count = db.execute(
        select(RunningPlanSession).where(RunningPlanSession.goal_id == goal.id)
    ).scalars().all()
    if existing_count:
        return 0

    today = datetime.now(timezone.utc)
    current = today + timedelta(days=(0 - today.weekday()) % 7)
    current = current.replace(hour=19, minute=0, second=0, microsecond=0)
    race_date = goal.race_date if goal.race_date.tzinfo else goal.race_date.replace(tzinfo=timezone.utc)
    profile = _progression_profile(goal.progression_style)

    created = 0
    week = 0
    while current.date() <= race_date.date():
        week += 1
        for plan_def in _session_blueprint(goal, week, profile):
            scheduled = current + timedelta(days=int(plan_def["day_offset"]))
            if scheduled.date() > race_date.date():
                continue
            distance_km = plan_def["distance_km"]
            pace = int(plan_def["pace"])
            speed = _speed_from_pace(pace)
            duration = int((Decimal(pace) * distance_km).to_integral_value(rounding=ROUND_HALF_UP))
            session = RunningPlanSession(
                user_id=goal.user_id,
                goal_id=goal.id,
                session_type=str(plan_def["session_type"]),
                title=str(plan_def["title"]),
                scheduled_date=scheduled,
                description=str(plan_def["description"]),
                target_distance_km=distance_km,
                target_duration_seconds=duration,
                target_pace_seconds_per_km=pace,
                target_speed_kmh=speed,
                status="planned",
            )
            db.add(session)
            db.flush()
            if session.session_type == "interval":
                db.add_all(_interval_steps(session.id, week, profile))
            else:
                db.add_all(_continuous_steps(session.id, pace, speed, distance_km, duration, session.session_type))
            created += 1
        current += timedelta(days=7)
    return created


def _continuous_steps(session_id: int, pace: int, speed: Decimal | None, distance_km: Decimal, duration: int, session_type: str) -> list[RunningWorkoutStep]:
    main_title = {
        "easy": "Rodagem",
        "recovery": "Rodagem regenerativa",
        "long": "Rodagem longa",
    }.get(session_type, "Bloco principal")
    return [
        RunningWorkoutStep(
            running_plan_session_id=session_id,
            order_index=1,
            step_type="warmup",
            title="Aquecimento",
            target_distance_m=800,
            target_duration_seconds=312,
            target_pace_seconds_per_km=390,
            target_speed_kmh=_speed_from_pace(390),
            notes="Aquecimento estruturado por distância. Use o tempo apenas como estimativa pela velocidade.",
        ),
        RunningWorkoutStep(
            running_plan_session_id=session_id,
            order_index=2,
            step_type="run",
            title=main_title,
            target_distance_m=int(distance_km * Decimal(1000)),
            target_duration_seconds=duration,
            target_pace_seconds_per_km=pace,
            target_speed_kmh=speed,
            notes="Use + e - para ajustar em 0,1 km/h; o tempo restante será recalculado.",
        ),
        RunningWorkoutStep(
            running_plan_session_id=session_id,
            order_index=3,
            step_type="cooldown",
            title="Desaquecimento",
            target_distance_m=600,
            target_duration_seconds=252,
            target_pace_seconds_per_km=420,
            target_speed_kmh=_speed_from_pace(420),
            notes="Desaquecimento por distância, reduzindo gradualmente.",
        ),
    ]


def _interval_steps(session_id: int, week: int, profile: dict | None = None) -> list[RunningWorkoutStep]:
    profile = profile or _progression_profile("balanced")
    reps = _interval_reps(week, profile)
    pace = _interval_pace(week, profile)
    speed = _speed_from_pace(pace)
    steps = [
        RunningWorkoutStep(
            running_plan_session_id=session_id,
            order_index=1,
            step_type="warmup",
            title="Aquecimento",
            target_distance_m=1000,
            target_duration_seconds=390,
            target_pace_seconds_per_km=390,
            target_speed_kmh=_speed_from_pace(390),
            notes="Aquecimento por distância antes dos tiros.",
        )
    ]
    order = 2
    for rep in range(1, reps + 1):
        steps.append(
            RunningWorkoutStep(
                running_plan_session_id=session_id,
                order_index=order,
                step_type="interval",
                title=f"Tiro {rep}",
                target_distance_m=400,
                target_duration_seconds=pace * 400 // 1000,
                target_pace_seconds_per_km=pace,
                target_speed_kmh=speed,
                notes="Mantenha forte, mas controlado.",
            )
        )
        order += 1
        if rep < reps:
            steps.append(
                RunningWorkoutStep(
                    running_plan_session_id=session_id,
                    order_index=order,
                    step_type="recovery",
                    title=f"Recuperação {rep}",
                    target_duration_seconds=90,
                    target_pace_seconds_per_km=600,
                    target_speed_kmh=Decimal("6.00"),
                    rest_seconds=90,
                    notes="Recuperação ativa na esteira.",
                )
            )
            order += 1
    steps.append(
        RunningWorkoutStep(
            running_plan_session_id=session_id,
            order_index=order,
            step_type="cooldown",
            title="Desaquecimento",
            target_distance_m=800,
            target_duration_seconds=336,
            target_pace_seconds_per_km=420,
            target_speed_kmh=_speed_from_pace(420),
            notes="Finalize leve por distância.",
        )
    )
    return steps


@router.post("/running-goals", response_model=RunningGoalRead, status_code=201)
def create_running_goal(payload: RunningGoalCreate, db: Session = Depends(get_db)) -> RunningGoal:
    _ensure_user(db, payload.user_id)
    existing = _get_active_goal(db, payload.user_id)
    if existing is not None:
        existing.status = "archived"
    goal = RunningGoal(**payload.model_dump(), status="active")
    db.add(goal)
    db.commit()
    db.refresh(goal)
    return goal


@router.get("/running-goals/current", response_model=RunningGoalRead)
def get_current_running_goal(user_id: int, db: Session = Depends(get_db)) -> RunningGoal:
    _ensure_user(db, user_id)
    goal = _get_active_goal(db, user_id)
    if goal is None:
        raise HTTPException(status_code=404, detail="No active running goal found")
    return goal


@router.post("/running-goals/{goal_id}/generate-plan", response_model=RunningPlanGenerateResponse)
def generate_running_plan(goal_id: int, db: Session = Depends(get_db)) -> dict:
    goal = db.get(RunningGoal, goal_id)
    if goal is None:
        raise HTTPException(status_code=404, detail="Running goal not found")
    created = _build_default_plan(goal, db)
    db.commit()
    return {"goal_id": goal_id, "created_sessions": created, "message": "Plano de corrida gerado para esteira, com execução orientada por distância."}


@router.get("/running-plan/week", response_model=list[RunningPlanSessionRead])
def get_running_plan_week(user_id: int, reference_date: datetime | None = None, db: Session = Depends(get_db)) -> list[RunningPlanSession]:
    _ensure_user(db, user_id)
    ref = reference_date or datetime.now(timezone.utc)
    if ref.tzinfo is None:
        ref = ref.replace(tzinfo=timezone.utc)
    week_start, week_end = _week_bounds(ref)
    return list(
        db.execute(
            select(RunningPlanSession)
            .options(selectinload(RunningPlanSession.steps))
            .where(
                RunningPlanSession.user_id == user_id,
                RunningPlanSession.scheduled_date >= week_start,
                RunningPlanSession.scheduled_date < week_end,
            )
            .order_by(RunningPlanSession.scheduled_date, RunningPlanSession.id)
        )
        .scalars()
        .all()
    )


@router.get("/running-plan/sessions/{session_id}", response_model=RunningPlanSessionRead)
def get_running_plan_session(session_id: int, db: Session = Depends(get_db)) -> RunningPlanSession:
    session = db.execute(
        select(RunningPlanSession).options(selectinload(RunningPlanSession.steps)).where(RunningPlanSession.id == session_id)
    ).scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=404, detail="Running plan session not found")
    return session


@router.post("/running-plan/sessions/{session_id}/start", response_model=RunningExecutionRead, status_code=201)
def start_running_execution(session_id: int, db: Session = Depends(get_db)) -> RunningExecutionLog:
    session = db.get(RunningPlanSession, session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Running plan session not found")
    session.status = "in_progress"
    execution = RunningExecutionLog(running_plan_session_id=session.id, status="in_progress")
    db.add(execution)
    db.commit()
    db.refresh(execution)
    return execution


@router.post("/running-executions/{execution_id}/steps/{step_id}/start", response_model=RunningStepLogRead, status_code=201)
def start_running_step(execution_id: int, step_id: int, db: Session = Depends(get_db)) -> RunningStepLog:
    execution = db.get(RunningExecutionLog, execution_id)
    step = db.get(RunningWorkoutStep, step_id)
    if execution is None or step is None:
        raise HTTPException(status_code=404, detail="Execution or step not found")
    if step.running_plan_session_id != execution.running_plan_session_id:
        raise HTTPException(status_code=400, detail="Step does not belong to this execution")
    existing = db.execute(
        select(RunningStepLog).where(
            RunningStepLog.running_execution_log_id == execution_id,
            RunningStepLog.running_workout_step_id == step_id,
        )
    ).scalar_one_or_none()
    if existing is not None:
        return existing
    log = RunningStepLog(
        running_execution_log_id=execution_id,
        running_workout_step_id=step_id,
        planned_speed_kmh=step.target_speed_kmh,
        actual_speed_kmh=step.target_speed_kmh,
        completed=False,
    )
    db.add(log)
    db.commit()
    db.refresh(log)
    return log


def _adjust_speed(step_log_id: int, delta: Decimal, adjustment_type: str, db: Session) -> RunningSpeedAdjustment:
    step_log = db.get(RunningStepLog, step_log_id)
    if step_log is None:
        raise HTTPException(status_code=404, detail="Running step log not found")
    previous = step_log.actual_speed_kmh or step_log.planned_speed_kmh or Decimal("0.00")
    new_speed = max(Decimal("0.00"), (Decimal(previous) + delta).quantize(Decimal("0.01")))
    step_log.actual_speed_kmh = new_speed
    adjustment = RunningSpeedAdjustment(
        running_step_log_id=step_log.id,
        adjustment_type=adjustment_type,
        previous_speed_kmh=previous,
        new_speed_kmh=new_speed,
    )
    db.add(adjustment)
    db.commit()
    db.refresh(adjustment)
    return adjustment


@router.post("/running-step-logs/{step_log_id}/speed-up", response_model=RunningSpeedAdjustmentRead)
def speed_up(step_log_id: int, db: Session = Depends(get_db)) -> RunningSpeedAdjustment:
    return _adjust_speed(step_log_id, Decimal("0.10"), "increase", db)


@router.post("/running-step-logs/{step_log_id}/speed-down", response_model=RunningSpeedAdjustmentRead)
def speed_down(step_log_id: int, db: Session = Depends(get_db)) -> RunningSpeedAdjustment:
    return _adjust_speed(step_log_id, Decimal("-0.10"), "decrease", db)



@router.post("/running-step-logs/{step_log_id}/complete", response_model=RunningStepAdvanceRead)
def complete_running_step(step_log_id: int, db: Session = Depends(get_db)) -> dict:
    step_log = db.get(RunningStepLog, step_log_id)
    if step_log is None:
        raise HTTPException(status_code=404, detail="Running step log not found")

    execution = db.get(RunningExecutionLog, step_log.running_execution_log_id)
    step = db.get(RunningWorkoutStep, step_log.running_workout_step_id)
    if execution is None or step is None:
        raise HTTPException(status_code=404, detail="Execution or step not found")
    if step.running_plan_session_id != execution.running_plan_session_id:
        raise HTTPException(status_code=400, detail="Step does not belong to this execution")

    now = datetime.now(timezone.utc)
    if not step_log.completed:
        step_log.completed = True
        step_log.finished_at = now

    next_step = db.execute(
        select(RunningWorkoutStep)
        .where(
            RunningWorkoutStep.running_plan_session_id == step.running_plan_session_id,
            RunningWorkoutStep.order_index > step.order_index,
        )
        .order_by(RunningWorkoutStep.order_index.asc())
    ).scalars().first()

    session = db.get(RunningPlanSession, execution.running_plan_session_id)
    session_completed = next_step is None
    message = "Etapa concluída. Próxima etapa pronta."
    if session_completed:
        execution.status = "completed"
        execution.finished_at = execution.finished_at or now
        if session is not None:
            session.status = "completed"
            execution.total_distance_km = session.target_distance_km
            execution.total_duration_seconds = session.target_duration_seconds
            execution.average_speed_kmh = session.target_speed_kmh
            execution.average_pace_seconds_per_km = session.target_pace_seconds_per_km
        message = "Treino finalizado automaticamente ao concluir a última etapa."
    elif next_step is not None:
        message = f"Etapa concluída. Próxima etapa: {next_step.title}."

    db.commit()
    db.refresh(step_log)
    db.refresh(execution)
    if next_step is not None:
        db.refresh(next_step)

    return {
        "completed_step_log": step_log,
        "next_step": next_step,
        "execution": execution,
        "session_completed": session_completed,
        "message": message,
    }


@router.post("/running-executions/{execution_id}/finish", response_model=RunningExecutionRead)
def finish_running_execution(execution_id: int, db: Session = Depends(get_db)) -> RunningExecutionLog:
    execution = db.get(RunningExecutionLog, execution_id)
    if execution is None:
        raise HTTPException(status_code=404, detail="Running execution not found")
    session = db.get(RunningPlanSession, execution.running_plan_session_id)
    execution.status = "completed"
    execution.finished_at = datetime.now(timezone.utc)
    if session is not None:
        session.status = "completed"
        execution.total_distance_km = session.target_distance_km
        execution.total_duration_seconds = session.target_duration_seconds
        execution.average_speed_kmh = session.target_speed_kmh
        execution.average_pace_seconds_per_km = session.target_pace_seconds_per_km
    db.commit()
    db.refresh(execution)
    return execution
