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
from app.models.user import User  # noqa: F401
