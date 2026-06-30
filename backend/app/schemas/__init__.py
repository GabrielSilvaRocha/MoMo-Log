from app.schemas.exercise import (
    EquipmentRead,
    ExerciseAlternativeRead,
    ExerciseRead,
    MuscleGroupRead,
    UserGymEquipmentRead,
    UserGymEquipmentUpsert,
)
from app.schemas.running import (
    RunningActivityCreate,
    RunningActivityRead,
    StravaAccountRead,
    StravaStatusRead,
    StravaSyncResponse,
)
from app.schemas.analytics import (
    GoalCreate,
    GoalProgressUpdate,
    GoalRead,
    PersonalRecordRead,
    WeeklyStatisticsRead,
)
from app.schemas.user import (
    AuthenticatedUserRead,
    AuthTokenRead,
    UserCreate,
    UserLogin,
    UserPreferenceRead,
    UserPreferenceUpdate,
    UserProfileUpdate,
    UserRead,
)

__all__ = [
    "UserRead",
    "UserCreate",
    "UserLogin",
    "AuthTokenRead",
    "AuthenticatedUserRead",
    "UserProfileUpdate",
    "UserPreferenceRead",
    "UserPreferenceUpdate",
    "ExerciseRead",
    "EquipmentRead",
    "MuscleGroupRead",
    "ExerciseAlternativeRead",
    "UserGymEquipmentRead",
    "UserGymEquipmentUpsert",
    "StravaSyncResponse",
    "StravaStatusRead",
    "StravaAccountRead",
    "RunningActivityCreate",
    "RunningActivityRead",
    "GoalCreate",
    "GoalProgressUpdate",
    "GoalRead",
    "PersonalRecordRead",
    "WeeklyStatisticsRead",
]
