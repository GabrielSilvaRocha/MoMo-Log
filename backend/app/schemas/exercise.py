from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class EquipmentRead(BaseModel):
    id: int
    name: str
    category: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class MuscleGroupRead(BaseModel):
    id: int
    name: str
    body_region: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ExerciseRead(BaseModel):
    id: int
    name: str
    slug: str
    description: str | None = None
    execution_instructions: str | None = None
    difficulty: str
    exercise_type: str
    is_unilateral: bool
    is_compound: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ExerciseAlternativeRead(BaseModel):
    id: int
    exercise_id: int
    alternative_exercise_id: int
    alternative_exercise: ExerciseRead
    equivalence_score: int
    reason: str | None = None
    equipment_status: str | None = None
    is_default_suggestion: bool = True

    model_config = ConfigDict(from_attributes=True)


class UserGymEquipmentRead(BaseModel):
    id: int
    user_id: int
    equipment_id: int
    status: str
    notes: str | None = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class UserGymEquipmentUpsert(BaseModel):
    user_id: int = Field(..., ge=1)
    equipment_id: int = Field(..., ge=1)
    status: str = Field(..., pattern="^(available|unavailable|unknown|favorite|frequently_busy)$")
    notes: str | None = None
