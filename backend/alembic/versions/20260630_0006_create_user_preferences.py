"""create user preferences and demo auth credentials

Revision ID: 20260630_0006
Revises: 20260629_0005
Create Date: 2026-06-30
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20260630_0006"
down_revision: Union[str, None] = "20260629_0005"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

DEMO_PASSWORD_HASH = "pbkdf2_sha256$210000$demo_salt$bcf1abec73b77161f191a65b8d30e980bef513949ea294f46bebb148b697b789"


def upgrade() -> None:
    op.create_table(
        "user_preferences",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("default_running_source", sa.String(length=80), nullable=False, server_default="manual_treadmill"),
        sa.Column("preferred_training_days", sa.String(length=120), nullable=True),
        sa.Column("weekly_running_goal_km", sa.Integer(), nullable=True),
        sa.Column("weekly_strength_goal_sessions", sa.Integer(), nullable=True),
        sa.Column("gym_notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )
    op.create_index(op.f("ix_user_preferences_id"), "user_preferences", ["id"], unique=False)
    op.create_index(op.f("ix_user_preferences_user_id"), "user_preferences", ["user_id"], unique=False)

    op.execute(
        f"""
        UPDATE users
        SET password_hash = '{DEMO_PASSWORD_HASH}'
        WHERE email = 'gabriel.demo@mo2log.com.br' AND password_hash IS NULL;
        """
    )
    op.execute(
        """
        INSERT INTO user_preferences (
            user_id,
            default_running_source,
            preferred_training_days,
            weekly_running_goal_km,
            weekly_strength_goal_sessions,
            gym_notes
        )
        VALUES (
            1,
            'manual_treadmill',
            'seg,ter,qua,qui,sex',
            20,
            3,
            'Academia com possibilidade de equipamento ocupado; priorizar alternativas quando necessário.'
        )
        ON CONFLICT (user_id) DO NOTHING;
        """
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_user_preferences_user_id"), table_name="user_preferences")
    op.drop_index(op.f("ix_user_preferences_id"), table_name="user_preferences")
    op.drop_table("user_preferences")
