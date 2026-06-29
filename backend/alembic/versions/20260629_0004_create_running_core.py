"""create running core

Revision ID: 20260629_0004
Revises: 20260629_0003
Create Date: 2026-06-29
"""
from typing import Sequence, Union
from datetime import datetime, timezone

from alembic import op
import sqlalchemy as sa

revision: str = "20260629_0004"
down_revision: Union[str, None] = "20260629_0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "strava_accounts",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("strava_athlete_id", sa.String(length=80), nullable=False),
        sa.Column("access_token_encrypted", sa.Text(), nullable=True),
        sa.Column("refresh_token_encrypted", sa.Text(), nullable=True),
        sa.Column("token_expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("strava_athlete_id"),
    )
    op.create_index(op.f("ix_strava_accounts_id"), "strava_accounts", ["id"], unique=False)
    op.create_index(op.f("ix_strava_accounts_user_id"), "strava_accounts", ["user_id"], unique=False)
    op.create_index(op.f("ix_strava_accounts_strava_athlete_id"), "strava_accounts", ["strava_athlete_id"], unique=False)

    op.create_table(
        "running_activities",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("training_session_id", sa.Integer(), nullable=True),
        sa.Column("strava_account_id", sa.Integer(), nullable=True),
        sa.Column("strava_activity_id", sa.String(length=80), nullable=True),
        sa.Column("name", sa.String(length=180), nullable=False),
        sa.Column("distance_m", sa.Numeric(10, 2), nullable=False),
        sa.Column("moving_time_s", sa.Integer(), nullable=False),
        sa.Column("elapsed_time_s", sa.Integer(), nullable=False),
        sa.Column("average_speed", sa.Numeric(10, 4), nullable=True),
        sa.Column("average_pace", sa.Numeric(8, 2), nullable=True),
        sa.Column("max_speed", sa.Numeric(10, 4), nullable=True),
        sa.Column("total_elevation_gain", sa.Numeric(8, 2), nullable=True),
        sa.Column("activity_type", sa.String(length=40), nullable=False),
        sa.Column("source", sa.String(length=40), nullable=False),
        sa.Column("start_date", sa.DateTime(timezone=True), nullable=False),
        sa.Column("raw_payload", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["strava_account_id"], ["strava_accounts.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["training_session_id"], ["training_sessions.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("strava_activity_id"),
        sa.UniqueConstraint("training_session_id", name="uq_running_activity_training_session"),
    )
    op.create_index(op.f("ix_running_activities_id"), "running_activities", ["id"], unique=False)
    op.create_index(op.f("ix_running_activities_user_id"), "running_activities", ["user_id"], unique=False)
    op.create_index(op.f("ix_running_activities_training_session_id"), "running_activities", ["training_session_id"], unique=False)
    op.create_index(op.f("ix_running_activities_strava_account_id"), "running_activities", ["strava_account_id"], unique=False)
    op.create_index(op.f("ix_running_activities_strava_activity_id"), "running_activities", ["strava_activity_id"], unique=False)
    op.create_index(op.f("ix_running_activities_start_date"), "running_activities", ["start_date"], unique=False)

    op.create_table(
        "strava_sync_logs",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("finished_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("imported_count", sa.Integer(), nullable=False),
        sa.Column("updated_count", sa.Integer(), nullable=False),
        sa.Column("ignored_count", sa.Integer(), nullable=False),
        sa.Column("status", sa.String(length=40), nullable=False),
        sa.Column("message", sa.Text(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_strava_sync_logs_id"), "strava_sync_logs", ["id"], unique=False)
    op.create_index(op.f("ix_strava_sync_logs_user_id"), "strava_sync_logs", ["user_id"], unique=False)

    strava_accounts = sa.table(
        "strava_accounts",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("strava_athlete_id", sa.String),
        sa.column("access_token_encrypted", sa.Text),
        sa.column("refresh_token_encrypted", sa.Text),
        sa.column("token_expires_at", sa.DateTime(timezone=True)),
    )
    running_activities = sa.table(
        "running_activities",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("training_session_id", sa.Integer),
        sa.column("strava_account_id", sa.Integer),
        sa.column("strava_activity_id", sa.String),
        sa.column("name", sa.String),
        sa.column("distance_m", sa.Numeric),
        sa.column("moving_time_s", sa.Integer),
        sa.column("elapsed_time_s", sa.Integer),
        sa.column("average_speed", sa.Numeric),
        sa.column("average_pace", sa.Numeric),
        sa.column("max_speed", sa.Numeric),
        sa.column("total_elevation_gain", sa.Numeric),
        sa.column("activity_type", sa.String),
        sa.column("source", sa.String),
        sa.column("start_date", sa.DateTime(timezone=True)),
    )

    op.bulk_insert(
        strava_accounts,
        [
            {
                "id": 1,
                "user_id": 1,
                "strava_athlete_id": "demo-athlete-1",
                "access_token_encrypted": None,
                "refresh_token_encrypted": None,
                "token_expires_at": None,
            }
        ],
    )
    op.bulk_insert(
        running_activities,
        [
            {
                "id": 1,
                "user_id": 1,
                "training_session_id": 2,
                "strava_account_id": 1,
                "strava_activity_id": "demo-strava-2",
                "name": "Rodagem leve",
                "distance_m": 5000,
                "moving_time_s": 1800,
                "elapsed_time_s": 1860,
                "average_speed": 2.7778,
                "average_pace": 6.00,
                "max_speed": 3.6000,
                "total_elevation_gain": 25,
                "activity_type": "Run",
                "source": "strava_mock",
                "start_date": datetime(2026, 6, 30, 10, 0, tzinfo=timezone.utc),
            }
        ],
    )
    op.execute("UPDATE training_sessions SET status = 'completed' WHERE id = 2;")
    op.execute("SELECT setval(pg_get_serial_sequence('strava_accounts','id'), (SELECT MAX(id) FROM strava_accounts));")
    op.execute("SELECT setval(pg_get_serial_sequence('running_activities','id'), (SELECT MAX(id) FROM running_activities));")


def downgrade() -> None:
    op.drop_index(op.f("ix_strava_sync_logs_user_id"), table_name="strava_sync_logs")
    op.drop_index(op.f("ix_strava_sync_logs_id"), table_name="strava_sync_logs")
    op.drop_table("strava_sync_logs")

    op.drop_index(op.f("ix_running_activities_start_date"), table_name="running_activities")
    op.drop_index(op.f("ix_running_activities_strava_activity_id"), table_name="running_activities")
    op.drop_index(op.f("ix_running_activities_strava_account_id"), table_name="running_activities")
    op.drop_index(op.f("ix_running_activities_training_session_id"), table_name="running_activities")
    op.drop_index(op.f("ix_running_activities_user_id"), table_name="running_activities")
    op.drop_index(op.f("ix_running_activities_id"), table_name="running_activities")
    op.drop_table("running_activities")

    op.drop_index(op.f("ix_strava_accounts_strava_athlete_id"), table_name="strava_accounts")
    op.drop_index(op.f("ix_strava_accounts_user_id"), table_name="strava_accounts")
    op.drop_index(op.f("ix_strava_accounts_id"), table_name="strava_accounts")
    op.drop_table("strava_accounts")
