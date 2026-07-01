"""running goal progression preferences

Revision ID: 20260701_0011
Revises: 20260630_0010
Create Date: 2026-07-01
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20260701_0011"
down_revision: Union[str, None] = "20260630_0010"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("running_goals", sa.Column("weekly_sessions", sa.Integer(), server_default="3", nullable=False))
    op.add_column("running_goals", sa.Column("progression_style", sa.String(length=40), server_default="balanced", nullable=False))
    op.add_column("running_goals", sa.Column("long_run_weekday", sa.String(length=10), server_default="fri", nullable=False))
    for table_name in (
        "running_goals",
        "running_plan_sessions",
        "running_workout_steps",
        "running_execution_logs",
        "running_step_logs",
        "running_speed_adjustments",
    ):
        op.execute(f"""
            SELECT setval(
                pg_get_serial_sequence('{table_name}', 'id'),
                COALESCE((SELECT MAX(id) FROM {table_name}), 1),
                true
            );
        """)


def downgrade() -> None:
    op.drop_column("running_goals", "long_run_weekday")
    op.drop_column("running_goals", "progression_style")
    op.drop_column("running_goals", "weekly_sessions")
