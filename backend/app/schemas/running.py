from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, Field


class RunningActivityRead(BaseModel):
    id: int
    user_id: int
    training_session_id: int | None = None
    strava_account_id: int | None = None
    strava_activity_id: str | None = None
    name: str
    distance_m: Decimal
    moving_time_s: int
    elapsed_time_s: int
    average_speed: Decimal | None = None
    average_pace: Decimal | None = None
    max_speed: Decimal | None = None
    total_elevation_gain: Decimal | None = None
    activity_type: str
    source: str
    start_date: datetime
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class RunningActivityCreate(BaseModel):
    user_id: int = Field(..., ge=1)
    training_session_id: int | None = Field(default=None, ge=1)
    name: str = Field(..., min_length=2, max_length=180)
    distance_m: Decimal = Field(..., gt=0)
    moving_time_s: int = Field(..., gt=0)
    elapsed_time_s: int = Field(..., gt=0)
    average_speed: Decimal | None = None
    average_pace: Decimal | None = None
    max_speed: Decimal | None = None
    total_elevation_gain: Decimal | None = None
    activity_type: str = "Run"
    source: str = "manual"
    start_date: datetime


class StravaAccountRead(BaseModel):
    id: int
    user_id: int
    strava_athlete_id: str
    token_expires_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class StravaStatusRead(BaseModel):
    user_id: int
    connected: bool
    strava_athlete_id: str | None = None
    token_expires_at: datetime | None = None


class StravaSyncResponse(BaseModel):
    imported: int
    updated: int
    ignored: int
    status: str
    message: str


class RunningGoalCreate(BaseModel):
    user_id: int = Field(..., ge=1)
    goal_type: str = "race"
    race_distance_km: Decimal = Field(default=Decimal("5.00"), gt=0)
    race_date: datetime
    current_5k_time_seconds: int | None = Field(default=None, gt=0)
    target_5k_time_seconds: int | None = Field(default=None, gt=0)
    training_location: str = "treadmill"
    available_weekdays: str = "mon,tue,wed,thu,fri"


class RunningGoalRead(BaseModel):
    id: int
    user_id: int
    goal_type: str
    race_distance_km: Decimal
    race_date: datetime
    current_5k_time_seconds: int | None = None
    target_5k_time_seconds: int | None = None
    training_location: str
    available_weekdays: str
    status: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class RunningWorkoutStepRead(BaseModel):
    id: int
    running_plan_session_id: int
    order_index: int
    step_type: str
    title: str
    target_distance_m: int | None = None
    target_duration_seconds: int | None = None
    target_pace_seconds_per_km: int | None = None
    target_speed_kmh: Decimal | None = None
    rest_seconds: int | None = None
    notes: str | None = None

    model_config = ConfigDict(from_attributes=True)


class RunningPlanSessionRead(BaseModel):
    id: int
    user_id: int
    goal_id: int
    training_session_id: int | None = None
    session_type: str
    title: str
    scheduled_date: datetime
    description: str | None = None
    target_distance_km: Decimal | None = None
    target_duration_seconds: int | None = None
    target_pace_seconds_per_km: int | None = None
    target_speed_kmh: Decimal | None = None
    status: str
    steps: list[RunningWorkoutStepRead] = []

    model_config = ConfigDict(from_attributes=True)


class RunningPlanGenerateResponse(BaseModel):
    goal_id: int
    created_sessions: int
    message: str


class RunningExecutionRead(BaseModel):
    id: int
    running_plan_session_id: int
    started_at: datetime
    finished_at: datetime | None = None
    status: str
    total_distance_km: Decimal | None = None
    total_duration_seconds: int | None = None
    average_speed_kmh: Decimal | None = None
    average_pace_seconds_per_km: int | None = None

    model_config = ConfigDict(from_attributes=True)


class RunningStepLogRead(BaseModel):
    id: int
    running_execution_log_id: int
    running_workout_step_id: int
    planned_speed_kmh: Decimal | None = None
    actual_speed_kmh: Decimal | None = None
    started_at: datetime
    finished_at: datetime | None = None
    completed: bool

    model_config = ConfigDict(from_attributes=True)


class RunningSpeedAdjustmentRead(BaseModel):
    id: int
    running_step_log_id: int
    adjustment_type: str
    previous_speed_kmh: Decimal | None = None
    new_speed_kmh: Decimal | None = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class RunningStepAdvanceRead(BaseModel):
    completed_step_log: RunningStepLogRead
    next_step: RunningWorkoutStepRead | None = None
    execution: RunningExecutionRead
    session_completed: bool
    message: str
