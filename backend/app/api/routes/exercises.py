from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.database.dependencies import get_db
from app.models.exercise import (
    Equipment,
    Exercise,
    ExerciseAlternative,
    ExerciseEquipment,
    MuscleGroup,
    UserGymEquipment,
)
from app.schemas.exercise import (
    EquipmentRead,
    ExerciseRead,
    MuscleGroupRead,
)

router = APIRouter(tags=["exercise-library"])


@router.get("/exercises", response_model=list[ExerciseRead])
def list_exercises(
    difficulty: str | None = None,
    exercise_type: str | None = None,
    db: Session = Depends(get_db),
) -> list[Exercise]:
    query = select(Exercise).order_by(Exercise.name)

    if difficulty:
        query = query.where(Exercise.difficulty == difficulty)
    if exercise_type:
        query = query.where(Exercise.exercise_type == exercise_type)

    result = db.execute(query)
    return list(result.scalars().all())


@router.get("/exercises/{exercise_id}", response_model=ExerciseRead)
def get_exercise(exercise_id: int, db: Session = Depends(get_db)) -> Exercise:
    exercise = db.get(Exercise, exercise_id)
    if exercise is None:
        raise HTTPException(status_code=404, detail="Exercise not found")
    return exercise


@router.get("/exercises/{exercise_id}/alternatives")
def list_exercise_alternatives(
    exercise_id: int,
    mode: str = Query("default", pattern="^(default|all)$"),
    user_id: int | None = None,
    db: Session = Depends(get_db),
) -> list[dict]:
    exercise = db.get(Exercise, exercise_id)
    if exercise is None:
        raise HTTPException(status_code=404, detail="Exercise not found")

    alternatives = db.execute(
        select(ExerciseAlternative)
        .options(joinedload(ExerciseAlternative.alternative_exercise))
        .where(ExerciseAlternative.exercise_id == exercise_id)
        .order_by(ExerciseAlternative.equivalence_score.desc())
    ).scalars().all()

    unavailable_equipment_ids: set[int] = set()
    frequently_busy_equipment_ids: set[int] = set()
    favorite_equipment_ids: set[int] = set()

    if user_id is not None:
        user_equipment = db.execute(
            select(UserGymEquipment).where(UserGymEquipment.user_id == user_id)
        ).scalars().all()

        unavailable_equipment_ids = {
            item.equipment_id for item in user_equipment if item.status == "unavailable"
        }
        frequently_busy_equipment_ids = {
            item.equipment_id for item in user_equipment if item.status == "frequently_busy"
        }
        favorite_equipment_ids = {
            item.equipment_id for item in user_equipment if item.status == "favorite"
        }

    response: list[dict] = []

    for alternative in alternatives:
        required_equipment_ids = set(
            db.execute(
                select(ExerciseEquipment.equipment_id).where(
                    ExerciseEquipment.exercise_id == alternative.alternative_exercise_id,
                    ExerciseEquipment.is_required.is_(True),
                )
            ).scalars().all()
        )

        is_unavailable = bool(required_equipment_ids & unavailable_equipment_ids)
        is_frequently_busy = bool(required_equipment_ids & frequently_busy_equipment_ids)
        is_favorite = bool(required_equipment_ids & favorite_equipment_ids)

        equipment_status = None
        is_default_suggestion = True

        if is_unavailable:
            equipment_status = "unavailable"
            is_default_suggestion = False
        elif is_frequently_busy:
            equipment_status = "frequently_busy"
        elif is_favorite:
            equipment_status = "favorite"

        if mode == "default" and not is_default_suggestion:
            continue

        response.append(
            {
                "id": alternative.id,
                "exercise_id": alternative.exercise_id,
                "alternative_exercise_id": alternative.alternative_exercise_id,
                "alternative_exercise": ExerciseRead.model_validate(
                    alternative.alternative_exercise
                ).model_dump(mode="json"),
                "equivalence_score": alternative.equivalence_score,
                "reason": alternative.reason,
                "equipment_status": equipment_status,
                "is_default_suggestion": is_default_suggestion,
            }
        )

    return response


@router.get("/equipment", response_model=list[EquipmentRead])
def list_equipment(db: Session = Depends(get_db)) -> list[Equipment]:
    result = db.execute(select(Equipment).order_by(Equipment.name))
    return list(result.scalars().all())


@router.get("/muscle-groups", response_model=list[MuscleGroupRead])
def list_muscle_groups(db: Session = Depends(get_db)) -> list[MuscleGroup]:
    result = db.execute(select(MuscleGroup).order_by(MuscleGroup.name))
    return list(result.scalars().all())
