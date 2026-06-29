"""create analytics core

Revision ID: 20260629_0005
Revises: 20260629_0004
Create Date: 2026-06-29
"""
from datetime import date, datetime, timezone
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20260629_0005"
down_revision: Union[str, None] = "20260629_0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "goals",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("goal_type", sa.String(length=80), nullable=False),
        sa.Column("title", sa.String(length=180), nullable=False),
        sa.Column("target_value", sa.Numeric(12, 2), nullable=False),
        sa.Column("current_value", sa.Numeric(12, 2), nullable=False),
        sa.Column("unit", sa.String(length=40), nullable=False),
        sa.Column("deadline", sa.Date(), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_goals_id"), "goals", ["id"], unique=False)
    op.create_index(op.f("ix_goals_user_id"), "goals", ["user_id"], unique=False)
    op.create_index(op.f("ix_goals_goal_type"), "goals", ["goal_type"], unique=False)
    op.create_index(op.f("ix_goals_deadline"), "goals", ["deadline"], unique=False)
    op.create_index(op.f("ix_goals_status"), "goals", ["status"], unique=False)

    op.create_table(
        "personal_records",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("record_type", sa.String(length=80), nullable=False),
        sa.Column("title", sa.String(length=180), nullable=False),
        sa.Column("value", sa.Numeric(12, 2), nullable=False),
        sa.Column("unit", sa.String(length=40), nullable=False),
        sa.Column("training_session_id", sa.Integer(), nullable=True),
        sa.Column("running_activity_id", sa.Integer(), nullable=True),
        sa.Column("achieved_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["running_activity_id"], ["running_activities.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["training_session_id"], ["training_sessions.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_personal_records_id"), "personal_records", ["id"], unique=False)
    op.create_index(op.f("ix_personal_records_user_id"), "personal_records", ["user_id"], unique=False)
    op.create_index(op.f("ix_personal_records_record_type"), "personal_records", ["record_type"], unique=False)
    op.create_index(op.f("ix_personal_records_training_session_id"), "personal_records", ["training_session_id"], unique=False)
    op.create_index(op.f("ix_personal_records_running_activity_id"), "personal_records", ["running_activity_id"], unique=False)
    op.create_index(op.f("ix_personal_records_achieved_at"), "personal_records", ["achieved_at"], unique=False)

    goals = sa.table(
        "goals",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("goal_type", sa.String),
        sa.column("title", sa.String),
        sa.column("target_value", sa.Numeric),
        sa.column("current_value", sa.Numeric),
        sa.column("unit", sa.String),
        sa.column("deadline", sa.Date),
        sa.column("status", sa.String),
    )
    personal_records = sa.table(
        "personal_records",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("record_type", sa.String),
        sa.column("title", sa.String),
        sa.column("value", sa.Numeric),
        sa.column("unit", sa.String),
        sa.column("training_session_id", sa.Integer),
        sa.column("running_activity_id", sa.Integer),
        sa.column("achieved_at", sa.DateTime(timezone=True)),
    )

    op.bulk_insert(
        goals,
        [
            {
                "id": 1,
                "user_id": 1,
                "goal_type": "weekly_running_distance",
                "title": "Correr 20 km na semana",
                "target_value": 20,
                "current_value": 5,
                "unit": "km",
                "deadline": date(2026, 7, 5),
                "status": "active",
            },
            {
                "id": 2,
                "user_id": 1,
                "goal_type": "weekly_strength_sessions",
                "title": "Concluir 3 treinos de musculação",
                "target_value": 3,
                "current_value": 0,
                "unit": "sessões",
                "deadline": date(2026, 7, 5),
                "status": "active",
            },
        ],
    )
    op.bulk_insert(
        personal_records,
        [
            {
                "id": 1,
                "user_id": 1,
                "record_type": "longest_run",
                "title": "Maior corrida registrada",
                "value": 5.00,
                "unit": "km",
                "training_session_id": 2,
                "running_activity_id": 1,
                "achieved_at": datetime(2026, 6, 30, 10, 0, tzinfo=timezone.utc),
            },
            {
                "id": 2,
                "user_id": 1,
                "record_type": "best_5k_time",
                "title": "Melhor tempo nos 5 km",
                "value": 1800,
                "unit": "segundos",
                "training_session_id": 2,
                "running_activity_id": 1,
                "achieved_at": datetime(2026, 6, 30, 10, 0, tzinfo=timezone.utc),
            },
        ],
    )
    op.execute("SELECT setval(pg_get_serial_sequence('goals','id'), (SELECT MAX(id) FROM goals));")
    op.execute("SELECT setval(pg_get_serial_sequence('personal_records','id'), (SELECT MAX(id) FROM personal_records));")


def downgrade() -> None:
    op.drop_index(op.f("ix_personal_records_achieved_at"), table_name="personal_records")
    op.drop_index(op.f("ix_personal_records_running_activity_id"), table_name="personal_records")
    op.drop_index(op.f("ix_personal_records_training_session_id"), table_name="personal_records")
    op.drop_index(op.f("ix_personal_records_record_type"), table_name="personal_records")
    op.drop_index(op.f("ix_personal_records_user_id"), table_name="personal_records")
    op.drop_index(op.f("ix_personal_records_id"), table_name="personal_records")
    op.drop_table("personal_records")

    op.drop_index(op.f("ix_goals_status"), table_name="goals")
    op.drop_index(op.f("ix_goals_deadline"), table_name="goals")
    op.drop_index(op.f("ix_goals_goal_type"), table_name="goals")
    op.drop_index(op.f("ix_goals_user_id"), table_name="goals")
    op.drop_index(op.f("ix_goals_id"), table_name="goals")
    op.drop_table("goals")
