"""create workout templates

Revision ID: 20260630_0008
Revises: 20260630_0007
Create Date: 2026-06-30
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "20260630_0008"
down_revision: Union[str, None] = "20260630_0007"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "workout_templates",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("goal", sa.String(length=120), nullable=True),
        sa.Column("difficulty", sa.String(length=40), nullable=False, server_default="intermediate"),
        sa.Column("estimated_duration_minutes", sa.Integer(), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False, server_default="active"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_workout_templates_id"), "workout_templates", ["id"], unique=False)
    op.create_index(op.f("ix_workout_templates_user_id"), "workout_templates", ["user_id"], unique=False)
    op.create_index(op.f("ix_workout_templates_status"), "workout_templates", ["status"], unique=False)

    op.create_table(
        "workout_template_exercises",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("workout_template_id", sa.Integer(), nullable=False),
        sa.Column("exercise_id", sa.Integer(), nullable=False),
        sa.Column("order_index", sa.Integer(), nullable=False),
        sa.Column("planned_sets", sa.Integer(), nullable=False, server_default="3"),
        sa.Column("planned_reps", sa.String(length=40), nullable=False, server_default="8-12"),
        sa.Column("rest_seconds", sa.Integer(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["exercise_id"], ["exercises.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["workout_template_id"], ["workout_templates.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("workout_template_id", "order_index", name="uq_workout_template_exercise_order"),
    )
    op.create_index(op.f("ix_workout_template_exercises_id"), "workout_template_exercises", ["id"], unique=False)
    op.create_index(
        op.f("ix_workout_template_exercises_workout_template_id"),
        "workout_template_exercises",
        ["workout_template_id"],
        unique=False,
    )

    seed_templates()


def seed_templates() -> None:
    templates = sa.table(
        "workout_templates",
        sa.column("id", sa.Integer),
        sa.column("user_id", sa.Integer),
        sa.column("name", sa.String),
        sa.column("description", sa.Text),
        sa.column("goal", sa.String),
        sa.column("difficulty", sa.String),
        sa.column("estimated_duration_minutes", sa.Integer),
        sa.column("status", sa.String),
    )
    template_exercises = sa.table(
        "workout_template_exercises",
        sa.column("id", sa.Integer),
        sa.column("workout_template_id", sa.Integer),
        sa.column("exercise_id", sa.Integer),
        sa.column("order_index", sa.Integer),
        sa.column("planned_sets", sa.Integer),
        sa.column("planned_reps", sa.String),
        sa.column("rest_seconds", sa.Integer),
        sa.column("notes", sa.Text),
    )

    op.bulk_insert(
        templates,
        [
            {
                "id": 1,
                "user_id": 1,
                "name": "Pernas — Força e base",
                "description": "Template para treino de pernas com foco em compostos, volume controlado e alternativas fáceis.",
                "goal": "força",
                "difficulty": "intermediate",
                "estimated_duration_minutes": 60,
                "status": "active",
            },
            {
                "id": 2,
                "user_id": 1,
                "name": "Superior — Empurrar",
                "description": "Template para peito, ombros e tríceps usando barra e halteres.",
                "goal": "hipertrofia",
                "difficulty": "intermediate",
                "estimated_duration_minutes": 55,
                "status": "active",
            },
            {
                "id": 3,
                "user_id": 1,
                "name": "Costas + Core",
                "description": "Template com puxadas, remadas e estabilidade de core.",
                "goal": "hipertrofia",
                "difficulty": "beginner",
                "estimated_duration_minutes": 50,
                "status": "active",
            },
        ],
    )

    op.bulk_insert(
        template_exercises,
        [
            {"id": 1, "workout_template_id": 1, "exercise_id": 1, "order_index": 1, "planned_sets": 4, "planned_reps": "6-8", "rest_seconds": 120, "notes": "Priorizar carga com execução controlada."},
            {"id": 2, "workout_template_id": 1, "exercise_id": 5, "order_index": 2, "planned_sets": 3, "planned_reps": "10-12", "rest_seconds": 90, "notes": "Manter contração no topo."},
            {"id": 3, "workout_template_id": 1, "exercise_id": 4, "order_index": 3, "planned_sets": 3, "planned_reps": "8-10 por perna", "rest_seconds": 90, "notes": "Trocar por Smith se estiver sem equilíbrio."},
            {"id": 4, "workout_template_id": 2, "exercise_id": 6, "order_index": 1, "planned_sets": 4, "planned_reps": "6-8", "rest_seconds": 120, "notes": "Movimento principal do treino."},
            {"id": 5, "workout_template_id": 2, "exercise_id": 7, "order_index": 2, "planned_sets": 3, "planned_reps": "8-12", "rest_seconds": 90, "notes": "Amplitude controlada."},
            {"id": 6, "workout_template_id": 3, "exercise_id": 8, "order_index": 1, "planned_sets": 4, "planned_reps": "8-12", "rest_seconds": 90, "notes": "Foco em dorsal."},
            {"id": 7, "workout_template_id": 3, "exercise_id": 9, "order_index": 2, "planned_sets": 3, "planned_reps": "8-10", "rest_seconds": 120, "notes": "Manter coluna neutra."},
            {"id": 8, "workout_template_id": 3, "exercise_id": 10, "order_index": 3, "planned_sets": 3, "planned_reps": "30-45s", "rest_seconds": 60, "notes": "Controle respiratório."},
        ],
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_workout_template_exercises_workout_template_id"), table_name="workout_template_exercises")
    op.drop_index(op.f("ix_workout_template_exercises_id"), table_name="workout_template_exercises")
    op.drop_table("workout_template_exercises")
    op.drop_index(op.f("ix_workout_templates_status"), table_name="workout_templates")
    op.drop_index(op.f("ix_workout_templates_user_id"), table_name="workout_templates")
    op.drop_index(op.f("ix_workout_templates_id"), table_name="workout_templates")
    op.drop_table("workout_templates")
