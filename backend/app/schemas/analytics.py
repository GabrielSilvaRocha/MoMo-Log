from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field


class GoalRead(BaseModel):
    id: int
    user_id: int
    goal_type: str
    title: str
    target_value: Decimal
    current_value: Decimal
    unit: str
    deadline: date | None = None
    status: str
    progress_percentage: float | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class GoalCreate(BaseModel):
    user_id: int = Field(..., ge=1)
    goal_type: str = Field(..., min_length=2, max_length=80)
    title: str = Field(..., min_length=2, max_length=180)
    target_value: Decimal = Field(..., gt=0)
    current_value: Decimal = Field(default=0, ge=0)
    unit: str = Field(..., min_length=1, max_length=40)
    deadline: date | None = None
    status: str = Field(default="active", pattern="^(active|completed|paused|cancelled)$")


class GoalProgressUpdate(BaseModel):
    current_value: Decimal = Field(..., ge=0)
    status: str | None = Field(default=None, pattern="^(active|completed|paused|cancelled)$")


class PersonalRecordRead(BaseModel):
    id: int
    user_id: int
    record_type: str
    title: str
    value: Decimal
    unit: str
    training_session_id: int | None = None
    running_activity_id: int | None = None
    achieved_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class WeeklyStatisticsRead(BaseModel):
    user_id: int
    reference_date: date
    week_start: date
    week_end: date
    completed_sessions: int
    upcoming_sessions: int
    adapted_sessions: int
    strength_sessions_completed: int
    running_sessions_completed: int
    weekly_strength_volume: Decimal
    weekly_running_distance_km: Decimal
    completion_rate: float
    active_goals: list[GoalRead]
    personal_records: list[PersonalRecordRead]
    insights: list[str]
