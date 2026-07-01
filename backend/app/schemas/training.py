from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.exercise import ExerciseRead
from app.schemas.running import RunningActivityRead


class TrainingPlanRead(BaseModel):
    id: int
    user_id: int
    name: str
    goal: str | None = None
    start_date: date
    end_date: date | None = None
    status: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class StrengthSetLogRead(BaseModel):
    id: int
    strength_workout_exercise_id: int
    set_number: int
    reps: int
    load: Decimal
    rir: int | None = None
    rpe: int | None = None
    completed_at: datetime
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class StrengthSetLogCreate(BaseModel):
    strength_workout_exercise_id: int = Field(..., ge=1)
    set_number: int = Field(..., ge=1)
    reps: int = Field(..., ge=0)
    load: Decimal = Field(..., ge=0)
    rir: int | None = Field(default=None, ge=0, le=10)
    rpe: int | None = Field(default=None, ge=0, le=10)


class StrengthWorkoutExerciseRead(BaseModel):
    id: int
    training_session_id: int
    exercise_id: int
    order_index: int
    planned_sets: int
    planned_reps: str
    planned_load: Decimal | None = None
    rest_seconds: int | None = None
    notes: str | None = None
    exercise: ExerciseRead | None = None
    set_logs: list[StrengthSetLogRead] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TrainingSessionRead(BaseModel):
    id: int
    user_id: int
    training_plan_id: int | None = None
    session_type: str
    title: str
    scheduled_date: date
    started_at: datetime | None = None
    finished_at: datetime | None = None
    status: str
    source: str
    notes: str | None = None
    strength_exercises: list[StrengthWorkoutExerciseRead] = Field(default_factory=list)
    running_activity: RunningActivityRead | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TrainingSessionCreate(BaseModel):
    user_id: int = Field(..., ge=1)
    training_plan_id: int | None = Field(default=None, ge=1)
    session_type: str = Field(..., pattern="^(strength|running|mobility|rest)$")
    title: str = Field(..., min_length=2, max_length=160)
    scheduled_date: date
    source: str = "manual"
    notes: str | None = None


class TrainingSessionUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=2, max_length=160)
    scheduled_date: date | None = None
    status: str | None = Field(default=None, pattern="^(planned|in_progress|completed|skipped|rescheduled|adapted)$")
    source: str | None = None
    notes: str | None = None


class StrengthWorkoutExerciseCreate(BaseModel):
    exercise_id: int = Field(..., ge=1)
    order_index: int | None = Field(default=None, ge=1)
    planned_sets: int = Field(default=3, ge=1, le=12)
    planned_reps: str = Field(default="8-12", min_length=1, max_length=40)
    planned_load: Decimal | None = Field(default=None, ge=0)
    rest_seconds: int | None = Field(default=90, ge=0, le=600)
    notes: str | None = None


class TrainingSessionReschedule(BaseModel):
    new_date: date
    reason: str | None = None


class ExerciseSwapCreate(BaseModel):
    strength_workout_exercise_id: int = Field(..., ge=1)
    original_exercise_id: int = Field(..., ge=1)
    new_exercise_id: int = Field(..., ge=1)
    reason: str = Field(..., pattern="^(equipment_busy|equipment_unavailable|pain_discomfort|preference|manual_adjustment)$")


class ExerciseSwapRead(BaseModel):
    status: str
    equivalence_score: int | None = None
    message: str


class WeekDashboardRead(BaseModel):
    user_id: int
    completed_sessions: list[TrainingSessionRead]
    today_sessions: list[TrainingSessionRead]
    upcoming_sessions: list[TrainingSessionRead]
    weekly_strength_volume: Decimal
    weekly_running_distance_km: Decimal
    completion_rate: float


class StrengthLoadProgressionRead(BaseModel):
    user_id: int
    exercise_id: int
    sample_sets: int
    latest_load: Decimal | None = None
    latest_reps: int | None = None
    latest_rir: int | None = None
    latest_rpe: int | None = None
    average_load: Decimal | None = None
    best_recent_load: Decimal | None = None
    suggested_load: Decimal | None = None
    recommendation: str
    rationale: str
