from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.exercise import Exercise
from app.services.adaptation import build_adaptation_suggestions

router = APIRouter(tags=["adaptation-engine"])


@router.get("/adaptation/exercises/{exercise_id}/suggestions")
def get_adaptation_suggestions(
    exercise_id: int,
    user_id: int | None = None,
    mode: str = Query(default="default", pattern="^(default|all)$"),
    reason: str = Query(
        default="equipment_busy",
        pattern="^(equipment_busy|equipment_unavailable|pain_discomfort|preference|manual_adjustment)$",
    ),
    db: Session = Depends(get_db),
) -> list[dict]:
    exercise = db.get(Exercise, exercise_id)
    if exercise is None:
        raise HTTPException(status_code=404, detail="Exercise not found")

    return build_adaptation_suggestions(
        db=db,
        exercise_id=exercise_id,
        user_id=user_id,
        mode=mode,
        reason=reason,
    )
