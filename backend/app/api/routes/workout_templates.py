from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select, text
from sqlalchemy.orm import Session, selectinload

from app.database.dependencies import get_db
from app.models.exercise import Exercise
from app.models.training import StrengthWorkoutExercise, TrainingPlan, TrainingSession
from app.models.user import User
from app.models.workout_template import WorkoutTemplate, WorkoutTemplateExercise
from app.schemas.workout_template import (
    WorkoutTemplateCreate,
    WorkoutTemplateRead,
    WorkoutTemplateSchedulePayload,
    WorkoutTemplateScheduleResponse,
)

router = APIRouter(tags=["workout-templates"])


def _template_query():
    return select(WorkoutTemplate).options(
        selectinload(WorkoutTemplate.exercises).joinedload(WorkoutTemplateExercise.exercise)
    )


def _session_query(session_id: int):
    return (
        select(TrainingSession)
        .where(TrainingSession.id == session_id)
        .options(
            selectinload(TrainingSession.strength_exercises).selectinload(StrengthWorkoutExercise.set_logs),
            selectinload(TrainingSession.strength_exercises).joinedload(StrengthWorkoutExercise.exercise),
            selectinload(TrainingSession.running_activity),
        )
    )


def _sync_workout_template_sequences(db: Session) -> None:
    db.execute(
        text(
            "SELECT setval(pg_get_serial_sequence('workout_templates', 'id'), "
            "COALESCE((SELECT MAX(id) FROM workout_templates), 1), true)"
        )
    )
    db.execute(
        text(
            "SELECT setval(pg_get_serial_sequence('workout_template_exercises', 'id'), "
            "COALESCE((SELECT MAX(id) FROM workout_template_exercises), 1), true)"
        )
    )


@router.get("/workout-templates", response_model=list[WorkoutTemplateRead])
def list_workout_templates(user_id: int, db: Session = Depends(get_db)) -> list[WorkoutTemplate]:
    return list(
        db.execute(
            _template_query()
            .where(WorkoutTemplate.user_id == user_id, WorkoutTemplate.status == "active")
            .order_by(WorkoutTemplate.name)
        )
        .scalars()
        .all()
    )


@router.post("/workout-templates", response_model=WorkoutTemplateRead, status_code=201)
def create_workout_template(payload: WorkoutTemplateCreate, db: Session = Depends(get_db)) -> WorkoutTemplate:
    if db.get(User, payload.user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    requested_ids = [item.exercise_id for item in payload.exercises]
    existing_ids = set(db.execute(select(Exercise.id).where(Exercise.id.in_(requested_ids))).scalars().all())
    missing_ids = sorted(set(requested_ids) - existing_ids)
    if missing_ids:
        raise HTTPException(status_code=404, detail=f"Exercises not found: {', '.join(map(str, missing_ids))}")

    _sync_workout_template_sequences(db)

    template = WorkoutTemplate(
        user_id=payload.user_id,
        name=payload.name,
        description=payload.description,
        goal=payload.goal,
        difficulty=payload.difficulty,
        estimated_duration_minutes=payload.estimated_duration_minutes,
        status="active",
    )
    db.add(template)
    db.flush()

    for order_index, item in enumerate(payload.exercises, start=1):
        db.add(
            WorkoutTemplateExercise(
                workout_template_id=template.id,
                exercise_id=item.exercise_id,
                order_index=order_index,
                planned_sets=item.planned_sets,
                planned_reps=item.planned_reps,
                rest_seconds=item.rest_seconds,
                notes=item.notes,
            )
        )

    db.commit()
    return db.execute(_template_query().where(WorkoutTemplate.id == template.id)).scalar_one()


@router.get("/workout-templates/{template_id}", response_model=WorkoutTemplateRead)
def get_workout_template(template_id: int, db: Session = Depends(get_db)) -> WorkoutTemplate:
    template = db.execute(_template_query().where(WorkoutTemplate.id == template_id)).scalar_one_or_none()
    if template is None:
        raise HTTPException(status_code=404, detail="Workout template not found")
    return template


@router.delete("/workout-templates/{template_id}", status_code=204)
def archive_workout_template(template_id: int, user_id: int, db: Session = Depends(get_db)) -> None:
    template = db.get(WorkoutTemplate, template_id)
    if template is None:
        raise HTTPException(status_code=404, detail="Workout template not found")
    if template.user_id != user_id:
        raise HTTPException(status_code=403, detail="Template does not belong to user")

    template.status = "archived"
    db.commit()
    return None


@router.post("/workout-templates/{template_id}/schedule", response_model=WorkoutTemplateScheduleResponse, status_code=201)
def schedule_workout_template(
    template_id: int,
    payload: WorkoutTemplateSchedulePayload,
    db: Session = Depends(get_db),
) -> WorkoutTemplateScheduleResponse:
    if db.get(User, payload.user_id) is None:
        raise HTTPException(status_code=404, detail="User not found")

    if payload.training_plan_id is not None and db.get(TrainingPlan, payload.training_plan_id) is None:
        raise HTTPException(status_code=404, detail="Training plan not found")

    template = db.execute(_template_query().where(WorkoutTemplate.id == template_id)).scalar_one_or_none()
    if template is None:
        raise HTTPException(status_code=404, detail="Workout template not found")

    if template.user_id != payload.user_id:
        raise HTTPException(status_code=403, detail="Template does not belong to user")

    if not template.exercises:
        raise HTTPException(status_code=400, detail="Template has no exercises")

    session = TrainingSession(
        user_id=payload.user_id,
        training_plan_id=payload.training_plan_id,
        session_type="strength",
        title=payload.title or template.name,
        scheduled_date=payload.scheduled_date,
        status="planned",
        source="template",
        notes=payload.notes or f"Criado a partir do template: {template.name}",
    )
    db.add(session)
    db.flush()

    for item in template.exercises:
        if db.get(Exercise, item.exercise_id) is None:
            continue
        db.add(
            StrengthWorkoutExercise(
                training_session_id=session.id,
                exercise_id=item.exercise_id,
                order_index=item.order_index,
                planned_sets=item.planned_sets,
                planned_reps=item.planned_reps,
                rest_seconds=item.rest_seconds,
                notes=item.notes,
            )
        )

    db.commit()
    scheduled_session = db.execute(_session_query(session.id)).scalar_one()
    return WorkoutTemplateScheduleResponse(
        status="scheduled",
        message="Sessão criada a partir do template com sucesso.",
        session=scheduled_session,
    )
