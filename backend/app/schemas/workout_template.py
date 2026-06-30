from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.exercise import ExerciseRead
from app.schemas.training import TrainingSessionRead


class WorkoutTemplateExerciseRead(BaseModel):
    id: int
    workout_template_id: int
    exercise_id: int
    order_index: int
    planned_sets: int
    planned_reps: str
    rest_seconds: int | None = None
    notes: str | None = None
    exercise: ExerciseRead | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class WorkoutTemplateRead(BaseModel):
    id: int
    user_id: int
    name: str
    description: str | None = None
    goal: str | None = None
    difficulty: str
    estimated_duration_minutes: int | None = None
    status: str
    exercises: list[WorkoutTemplateExerciseRead] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class WorkoutTemplateSchedulePayload(BaseModel):
    user_id: int = Field(..., ge=1)
    scheduled_date: date
    training_plan_id: int | None = Field(default=None, ge=1)
    title: str | None = Field(default=None, min_length=2, max_length=160)
    notes: str | None = None


class WorkoutTemplateScheduleResponse(BaseModel):
    status: str
    message: str
    session: TrainingSessionRead
