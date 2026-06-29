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
