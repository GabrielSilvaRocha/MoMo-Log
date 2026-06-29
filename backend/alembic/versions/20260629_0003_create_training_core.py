"""create training core

Revision ID: 20260629_0003
Revises: 20260629_0002
Create Date: 2026-06-29
"""
from typing import Sequence, Union
from datetime import date

from alembic import op
import sqlalchemy as sa

revision: str = "20260629_0003"
down_revision: Union[str, None] = "20260629_0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "training_plans",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("goal", sa.String(length=160), nullable=True),
        sa.Column("start_date", sa.Date(), nullable=False),
        sa.Column("end_date", sa.Date(), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_training_plans_id"), "training_plans", ["id"], unique=False)
    op.create_index(op.f("ix_training_plans_user_id"), "training_plans", ["user_id"], unique=False)

    op.create_table(
        "training_sessions",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("training_plan_id", sa.Integer(), nullable=True),
        sa.Column("session_type", sa.String(length=40), nullable=False),
        sa.Column("title", sa.String(length=160), nullable=False),
        sa.Column("scheduled_date", sa.Date(), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("finished_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False),
        sa.Column("source", sa.String(length=40), nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["training_plan_id"], ["training_plans.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_training_sessions_id"), "training_sessions", ["id"], unique=False)
    op.create_index(op.f("ix_training_sessions_user_id"), "training_sessions", ["user_id"], unique=False)
    op.create_index(op.f("ix_training_sessions_training_plan_id"), "training_sessions", ["training_plan_id"], unique=False)
    op.create_index(op.f("ix_training_sessions_scheduled_date"), "training_sessions", ["scheduled_date"], unique=False)
    op.create_index(op.f("ix_training_sessions_status"), "training_sessions", ["status"], unique=False)

    op.create_table(
        "strength_workout_exercises",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("training_session_id", sa.Integer(), nullable=False),
        sa.Column("exercise_id", sa.Integer(), nullable=False),
        sa.Column("order_index", sa.Integer(), nullable=False),
        sa.Column("planned_sets", sa.Integer(), nullable=False),
        sa.Column("planned_reps", sa.String(length=40), nullable=False),
        sa.Column("planned_load", sa.Numeric(8, 2), nullable=True),
        sa.Column("rest_seconds", sa.Integer(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["exercise_id"], ["exercises.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["training_session_id"], ["training_sessions.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("training_session_id", "order_index", name="uq_strength_exercise_order"),
    )
    op.create_index(op.f("ix_strength_workout_exercises_id"), "strength_workout_exercises", ["id"], unique=False)
    op.create_index(op.f("ix_strength_workout_exercises_training_session_id"), "strength_workout_exercises", ["training_session_id"], unique=False)

    op.create_table(
        "strength_set_logs",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("strength_workout_exercise_id", sa.Integer(), nullable=False),
        sa.Column("set_number", sa.Integer(), nullable=False),
        sa.Column("reps", sa.Integer(), nullable=False),
        sa.Column("load", sa.Numeric(8, 2), nullable=False),
        sa.Column("rir", sa.Integer(), nullable=True),
        sa.Column("rpe", sa.Integer(), nullable=True),
        sa.Column("completed_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.CheckConstraint("reps >= 0", name="ck_strength_set_reps_positive"),
        sa.CheckConstraint("load >= 0", name="ck_strength_set_load_positive"),
        sa.CheckConstraint("rir IS NULL OR (rir >= 0 AND rir <= 10)", name="ck_strength_set_rir_range"),
        sa.CheckConstraint("rpe IS NULL OR (rpe >= 0 AND rpe <= 10)", name="ck_strength_set_rpe_range"),
        sa.ForeignKeyConstraint(["strength_workout_exercise_id"], ["strength_workout_exercises.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("strength_workout_exercise_id", "set_number", name="uq_strength_set_number"),
    )
    op.create_index(op.f("ix_strength_set_logs_id"), "strength_set_logs", ["id"], unique=False)
    op.create_index(op.f("ix_strength_set_logs_strength_workout_exercise_id"), "strength_set_logs", ["strength_workout_exercise_id"], unique=False)

    op.create_table(
        "exercise_swap_logs",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("training_session_id", sa.Integer(), nullable=False),
        sa.Column("original_exercise_id", sa.Integer(), nullable=False),
        sa.Column("new_exercise_id", sa.Integer(), nullable=False),
        sa.Column("reason", sa.String(length=80), nullable=False),
        sa.Column("equivalence_score", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(["new_exercise_id"], ["exercises.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["original_exercise_id"], ["exercises.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["training_session_id"], ["training_sessions.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_exercise_swap_logs_id"), "exercise_swap_logs", ["id"], unique=False)
    op.create_index(op.f("ix_exercise_swap_logs_training_session_id"), "exercise_swap_logs", ["training_session_id"], unique=False)

    seed_demo_training_data()


def seed_demo_training_data() -> None:
    op.execute(
        """
        INSERT INTO users (id, name, email, password_hash, avatar_url)
        VALUES (1, 'Gabriel Rocha', 'gabriel.demo@mo2log.com.br', NULL, NULL)
        ON CONFLICT (email) DO NOTHING;
        """
    )

    training_plans = sa.table(
        "training_plans",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("name", sa.String),
        sa.column("goal", sa.String),
        sa.column("start_date", sa.Date),
        sa.column("end_date", sa.Date),
        sa.column("status", sa.String),
    )
    training_sessions = sa.table(
        "training_sessions",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("training_plan_id", sa.Integer),
        sa.column("session_type", sa.String),
        sa.column("title", sa.String),
        sa.column("scheduled_date", sa.Date),
        sa.column("status", sa.String),
        sa.column("source", sa.String),
        sa.column("notes", sa.Text),
    )
    strength_workout_exercises = sa.table(
        "strength_workout_exercises",
        sa.column("id", sa.Integer),
        sa.column("training_session_id", sa.Integer),
        sa.column("exercise_id", sa.Integer),
        sa.column("order_index", sa.Integer),
        sa.column("planned_sets", sa.Integer),
        sa.column("planned_reps", sa.String),
        sa.column("planned_load", sa.Numeric),
        sa.column("rest_seconds", sa.Integer),
        sa.column("notes", sa.Text),
    )

    op.bulk_insert(
        training_plans,
        [
            {
                "id": 1,
                "user_id": 1,
                "name": "Plano Híbrido Base",
                "goal": "Construir consistência em musculação e corrida",
                "start_date": date(2026, 6, 29),
                "end_date": date(2026, 8, 16),
                "status": "active",
            }
        ],
    )
    op.bulk_insert(
        training_sessions,
        [
            {"id": 1, "user_id": 1, "training_plan_id": 1, "session_type": "strength", "title": "Pernas + Core", "scheduled_date": date(2026, 6, 29), "status": "planned", "source": "seed", "notes": "Ênfase em força."},
            {"id": 2, "user_id": 1, "training_plan_id": 1, "session_type": "running", "title": "Rodagem leve", "scheduled_date": date(2026, 6, 30), "status": "planned", "source": "seed", "notes": "Corrida será sincronizada pelo Strava futuramente."},
            {"id": 3, "user_id": 1, "training_plan_id": 1, "session_type": "strength", "title": "Peito + Ombros + Tríceps", "scheduled_date": date(2026, 7, 1), "status": "planned", "source": "seed", "notes": None},
            {"id": 4, "user_id": 1, "training_plan_id": 1, "session_type": "running", "title": "Intervalado curto", "scheduled_date": date(2026, 7, 2), "status": "planned", "source": "seed", "notes": "Planejado, sem integração Strava nesta release."},
            {"id": 5, "user_id": 1, "training_plan_id": 1, "session_type": "strength", "title": "Costas + Bíceps + Posterior", "scheduled_date": date(2026, 7, 3), "status": "planned", "source": "seed", "notes": None},
            {"id": 6, "user_id": 1, "training_plan_id": 1, "session_type": "rest", "title": "Descanso", "scheduled_date": date(2026, 7, 4), "status": "planned", "source": "seed", "notes": None},
            {"id": 7, "user_id": 1, "training_plan_id": 1, "session_type": "rest", "title": "Descanso", "scheduled_date": date(2026, 7, 5), "status": "planned", "source": "seed", "notes": None},
        ],
    )
    op.bulk_insert(
        strength_workout_exercises,
        [
            {"id": 1, "training_session_id": 1, "exercise_id": 1, "order_index": 1, "planned_sets": 4, "planned_reps": "8-10", "planned_load": 120, "rest_seconds": 120, "notes": "Principal do dia."},
            {"id": 2, "training_session_id": 1, "exercise_id": 5, "order_index": 2, "planned_sets": 3, "planned_reps": "10-12", "planned_load": 45, "rest_seconds": 90, "notes": None},
            {"id": 3, "training_session_id": 1, "exercise_id": 10, "order_index": 3, "planned_sets": 3, "planned_reps": "30-45s", "planned_load": 0, "rest_seconds": 60, "notes": "Core."},
            {"id": 4, "training_session_id": 3, "exercise_id": 6, "order_index": 1, "planned_sets": 4, "planned_reps": "6-8", "planned_load": 60, "rest_seconds": 120, "notes": None},
            {"id": 5, "training_session_id": 3, "exercise_id": 7, "order_index": 2, "planned_sets": 3, "planned_reps": "8-10", "planned_load": 24, "rest_seconds": 90, "notes": None},
            {"id": 6, "training_session_id": 5, "exercise_id": 8, "order_index": 1, "planned_sets": 4, "planned_reps": "8-12", "planned_load": 55, "rest_seconds": 90, "notes": None},
            {"id": 7, "training_session_id": 5, "exercise_id": 9, "order_index": 2, "planned_sets": 3, "planned_reps": "8-10", "planned_load": 50, "rest_seconds": 120, "notes": None},
        ],
    )

    op.execute("SELECT setval(pg_get_serial_sequence('users','id'), (SELECT MAX(id) FROM users));")
    op.execute("SELECT setval(pg_get_serial_sequence('training_plans','id'), (SELECT MAX(id) FROM training_plans));")
    op.execute("SELECT setval(pg_get_serial_sequence('training_sessions','id'), (SELECT MAX(id) FROM training_sessions));")
    op.execute("SELECT setval(pg_get_serial_sequence('strength_workout_exercises','id'), (SELECT MAX(id) FROM strength_workout_exercises));")


def downgrade() -> None:
    op.drop_index(op.f("ix_exercise_swap_logs_training_session_id"), table_name="exercise_swap_logs")
    op.drop_index(op.f("ix_exercise_swap_logs_id"), table_name="exercise_swap_logs")
    op.drop_table("exercise_swap_logs")

    op.drop_index(op.f("ix_strength_set_logs_strength_workout_exercise_id"), table_name="strength_set_logs")
    op.drop_index(op.f("ix_strength_set_logs_id"), table_name="strength_set_logs")
    op.drop_table("strength_set_logs")

    op.drop_index(op.f("ix_strength_workout_exercises_training_session_id"), table_name="strength_workout_exercises")
    op.drop_index(op.f("ix_strength_workout_exercises_id"), table_name="strength_workout_exercises")
    op.drop_table("strength_workout_exercises")

    op.drop_index(op.f("ix_training_sessions_status"), table_name="training_sessions")
    op.drop_index(op.f("ix_training_sessions_scheduled_date"), table_name="training_sessions")
    op.drop_index(op.f("ix_training_sessions_training_plan_id"), table_name="training_sessions")
    op.drop_index(op.f("ix_training_sessions_user_id"), table_name="training_sessions")
    op.drop_index(op.f("ix_training_sessions_id"), table_name="training_sessions")
    op.drop_table("training_sessions")

    op.drop_index(op.f("ix_training_plans_user_id"), table_name="training_plans")
    op.drop_index(op.f("ix_training_plans_id"), table_name="training_plans")
    op.drop_table("training_plans")
