from decimal import Decimal

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.dependencies import get_db
from app.models.analytics import Goal
from app.schemas.analytics import GoalCreate, GoalProgressUpdate, GoalRead

router = APIRouter(prefix="/goals", tags=["goals"])


def _goal_to_read(goal: Goal) -> GoalRead:
    progress_percentage = None
    if goal.target_value and Decimal(goal.target_value) > 0:
        progress_percentage = round((Decimal(goal.current_value) / Decimal(goal.target_value)) * 100, 2)
    return GoalRead.model_validate(goal).model_copy(update={"progress_percentage": float(progress_percentage) if progress_percentage is not None else None})


@router.get("", response_model=list[GoalRead])
def list_goals(user_id: int, status: str | None = None, db: Session = Depends(get_db)) -> list[GoalRead]:
    stmt = select(Goal).where(Goal.user_id == user_id).order_by(Goal.deadline.asc().nullslast(), Goal.id)
    if status:
        stmt = stmt.where(Goal.status == status)
    goals = db.execute(stmt).scalars().all()
    return [_goal_to_read(goal) for goal in goals]


@router.post("", response_model=GoalRead, status_code=201)
def create_goal(payload: GoalCreate, db: Session = Depends(get_db)) -> GoalRead:
    goal = Goal(**payload.model_dump())
    db.add(goal)
    db.commit()
    db.refresh(goal)
    return _goal_to_read(goal)


@router.patch("/{goal_id}/progress", response_model=GoalRead)
def update_goal_progress(goal_id: int, payload: GoalProgressUpdate, db: Session = Depends(get_db)) -> GoalRead:
    goal = db.get(Goal, goal_id)
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")

    goal.current_value = payload.current_value
    if payload.status:
        goal.status = payload.status
    elif Decimal(goal.current_value) >= Decimal(goal.target_value):
        goal.status = "completed"

    db.commit()
    db.refresh(goal)
    return _goal_to_read(goal)
