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


class WorkoutTemplateExerciseCreate(BaseModel):
    exercise_id: int = Field(..., ge=1)
    planned_sets: int = Field(default=3, ge=1, le=20)
    planned_reps: str = Field(default="8-12", min_length=1, max_length=40)
    rest_seconds: int | None = Field(default=90, ge=0, le=600)
    notes: str | None = Field(default=None, max_length=500)


class WorkoutTemplateCreate(BaseModel):
    user_id: int = Field(..., ge=1)
    name: str = Field(..., min_length=2, max_length=160)
    description: str | None = Field(default=None, max_length=1000)
    goal: str | None = Field(default=None, max_length=120)
    difficulty: str = Field(default="intermediate", pattern="^(beginner|intermediate|advanced)$")
    estimated_duration_minutes: int | None = Field(default=None, ge=5, le=240)
    exercises: list[WorkoutTemplateExerciseCreate] = Field(..., min_length=1, max_length=20)


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
