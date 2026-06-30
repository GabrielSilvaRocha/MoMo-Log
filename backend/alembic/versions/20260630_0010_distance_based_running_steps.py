"""distance based running steps

Revision ID: 20260630_0010
Revises: 20260630_0009
Create Date: 2026-06-30
"""
from typing import Sequence, Union

from alembic import op

revision: str = "20260630_0010"
down_revision: Union[str, None] = "20260630_0009"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # O plano de corrida passa a tratar distância como referência principal.
    # Etapas de descanso/recuperação continuam por tempo.
    op.execute("""
        UPDATE running_workout_steps
        SET target_distance_m = CASE
            WHEN step_type = 'warmup' AND COALESCE(target_duration_seconds, 0) <= 300 THEN 800
            WHEN step_type = 'warmup' THEN 1000
            WHEN step_type = 'cooldown' AND COALESCE(target_duration_seconds, 0) <= 300 THEN 600
            WHEN step_type = 'cooldown' AND COALESCE(target_duration_seconds, 0) <= 420 THEN 700
            WHEN step_type = 'cooldown' THEN 800
            ELSE target_distance_m
        END
        WHERE target_distance_m IS NULL
          AND step_type IN ('warmup', 'cooldown');
    """)


def downgrade() -> None:
    op.execute("""
        UPDATE running_workout_steps
        SET target_distance_m = NULL
        WHERE step_type IN ('warmup', 'cooldown')
          AND target_distance_m IN (600, 700, 800, 1000);
    """)
