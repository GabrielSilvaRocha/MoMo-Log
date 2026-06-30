from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class UserRead(BaseModel):
    id: int
    name: str
    email: EmailStr
    avatar_url: str | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class UserCreate(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=128)
    avatar_url: str | None = Field(default=None, max_length=500)


class UserLogin(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1, max_length=128)


class UserProfileUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=120)
    avatar_url: str | None = Field(default=None, max_length=500)


class UserPreferenceRead(BaseModel):
    id: int
    user_id: int
    default_running_source: str
    preferred_training_days: str | None = None
    weekly_running_goal_km: int | None = None
    weekly_strength_goal_sessions: int | None = None
    gym_notes: str | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class UserPreferenceUpdate(BaseModel):
    default_running_source: str | None = Field(default=None, pattern="^(running_coach|manual_treadmill|manual_outdoor|manual)$")
    preferred_training_days: str | None = Field(default=None, max_length=120)
    weekly_running_goal_km: int | None = Field(default=None, ge=0, le=300)
    weekly_strength_goal_sessions: int | None = Field(default=None, ge=0, le=14)
    gym_notes: str | None = Field(default=None, max_length=1000)


class AuthTokenRead(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserRead


class AuthenticatedUserRead(BaseModel):
    user: UserRead
    preferences: UserPreferenceRead | None = None
