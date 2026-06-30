"""Importa os models para o Alembic detectar o metadata.

Não importe este arquivo dentro dos models para evitar importação circular.
"""

from app.models.exercise import (  # noqa: F401
    Equipment,
    Exercise,
    ExerciseAlternative,
    ExerciseEquipment,
    ExerciseMuscle,
    MuscleGroup,
    UserGymEquipment,
)
from app.models.training import (  # noqa: F401
    ExerciseSwapLog,
    StrengthSetLog,
    StrengthWorkoutExercise,
    TrainingPlan,
    TrainingSession,
)
from app.models.running import (
    RunningActivity,
    StravaAccount,
    StravaSyncLog,
)
from app.models.user import User, UserPreference  # noqa: F401

from app.models.analytics import Goal, PersonalRecord  # noqa: F401
