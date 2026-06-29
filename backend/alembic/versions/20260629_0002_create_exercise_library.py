"""create exercise library

Revision ID: 20260629_0002
Revises: 20260629_0001
Create Date: 2026-06-29
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20260629_0002"
down_revision: Union[str, None] = "20260629_0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "exercises",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=160), nullable=False),
        sa.Column("slug", sa.String(length=180), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("execution_instructions", sa.Text(), nullable=True),
        sa.Column("difficulty", sa.String(length=40), nullable=False),
        sa.Column("exercise_type", sa.String(length=40), nullable=False),
        sa.Column("is_unilateral", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("is_compound", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_exercises_id"), "exercises", ["id"], unique=False)
    op.create_index(op.f("ix_exercises_slug"), "exercises", ["slug"], unique=True)

    op.create_table(
        "muscle_groups",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("body_region", sa.String(length=80), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_muscle_groups_id"), "muscle_groups", ["id"], unique=False)
    op.create_index(op.f("ix_muscle_groups_name"), "muscle_groups", ["name"], unique=True)

    op.create_table(
        "equipment",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=120), nullable=False),
        sa.Column("category", sa.String(length=80), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_equipment_id"), "equipment", ["id"], unique=False)
    op.create_index(op.f("ix_equipment_name"), "equipment", ["name"], unique=True)

    op.create_table(
        "exercise_muscles",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("exercise_id", sa.Integer(), nullable=False),
        sa.Column("muscle_group_id", sa.Integer(), nullable=False),
        sa.Column("role", sa.String(length=40), nullable=False),
        sa.Column("activation_level", sa.Integer(), nullable=False),
        sa.CheckConstraint("activation_level >= 0 AND activation_level <= 100", name="ck_activation_level_range"),
        sa.ForeignKeyConstraint(["exercise_id"], ["exercises.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["muscle_group_id"], ["muscle_groups.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("exercise_id", "muscle_group_id", "role", name="uq_exercise_muscle_role"),
    )
    op.create_index(op.f("ix_exercise_muscles_id"), "exercise_muscles", ["id"], unique=False)

    op.create_table(
        "exercise_equipment",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("exercise_id", sa.Integer(), nullable=False),
        sa.Column("equipment_id", sa.Integer(), nullable=False),
        sa.Column("is_required", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.ForeignKeyConstraint(["equipment_id"], ["equipment.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["exercise_id"], ["exercises.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("exercise_id", "equipment_id", name="uq_exercise_equipment"),
    )
    op.create_index(op.f("ix_exercise_equipment_id"), "exercise_equipment", ["id"], unique=False)

    op.create_table(
        "exercise_alternatives",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("exercise_id", sa.Integer(), nullable=False),
        sa.Column("alternative_exercise_id", sa.Integer(), nullable=False),
        sa.Column("equivalence_score", sa.Integer(), nullable=False),
        sa.Column("reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("equivalence_score >= 0 AND equivalence_score <= 100", name="ck_equivalence_score_range"),
        sa.ForeignKeyConstraint(["alternative_exercise_id"], ["exercises.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["exercise_id"], ["exercises.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("exercise_id", "alternative_exercise_id", name="uq_exercise_alternative"),
    )
    op.create_index(op.f("ix_exercise_alternatives_id"), "exercise_alternatives", ["id"], unique=False)

    op.create_table(
        "user_gym_equipment",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("equipment_id", sa.Integer(), nullable=False),
        sa.Column("status", sa.String(length=40), nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["equipment_id"], ["equipment.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "equipment_id", name="uq_user_gym_equipment"),
    )
    op.create_index(op.f("ix_user_gym_equipment_id"), "user_gym_equipment", ["id"], unique=False)

    seed_reference_data()


def seed_reference_data() -> None:
    equipment = sa.table(
        "equipment",
        sa.column("id", sa.Integer),
        sa.column("name", sa.String),
        sa.column("category", sa.String),
    )
    muscle_groups = sa.table(
        "muscle_groups",
        sa.column("id", sa.Integer),
        sa.column("name", sa.String),
        sa.column("body_region", sa.String),
    )
    exercises = sa.table(
        "exercises",
        sa.column("id", sa.Integer),
        sa.column("name", sa.String),
        sa.column("slug", sa.String),
        sa.column("description", sa.Text),
        sa.column("execution_instructions", sa.Text),
        sa.column("difficulty", sa.String),
        sa.column("exercise_type", sa.String),
        sa.column("is_unilateral", sa.Boolean),
        sa.column("is_compound", sa.Boolean),
    )
    exercise_muscles = sa.table(
        "exercise_muscles",
        sa.column("id", sa.Integer),
        sa.column("exercise_id", sa.Integer),
        sa.column("muscle_group_id", sa.Integer),
        sa.column("role", sa.String),
        sa.column("activation_level", sa.Integer),
    )
    exercise_equipment = sa.table(
        "exercise_equipment",
        sa.column("id", sa.Integer),
        sa.column("exercise_id", sa.Integer),
        sa.column("equipment_id", sa.Integer),
        sa.column("is_required", sa.Boolean),
    )
    exercise_alternatives = sa.table(
        "exercise_alternatives",
        sa.column("id", sa.Integer),
        sa.column("exercise_id", sa.Integer),
        sa.column("alternative_exercise_id", sa.Integer),
        sa.column("equivalence_score", sa.Integer),
        sa.column("reason", sa.Text),
    )

    op.bulk_insert(
        equipment,
        [
            {"id": 1, "name": "Halter", "category": "peso livre"},
            {"id": 2, "name": "Barra", "category": "peso livre"},
            {"id": 3, "name": "Smith", "category": "máquina"},
            {"id": 4, "name": "Leg Press 45°", "category": "máquina"},
            {"id": 5, "name": "Hack Machine", "category": "máquina"},
            {"id": 6, "name": "Banco", "category": "acessório"},
            {"id": 7, "name": "Polia", "category": "máquina"},
            {"id": 8, "name": "Peso corporal", "category": "livre"},
            {"id": 9, "name": "Cadeira extensora", "category": "máquina"},
            {"id": 10, "name": "Esteira", "category": "cardio"},
        ],
    )
    op.bulk_insert(
        muscle_groups,
        [
            {"id": 1, "name": "Quadríceps", "body_region": "pernas"},
            {"id": 2, "name": "Glúteos", "body_region": "pernas"},
            {"id": 3, "name": "Posterior de coxa", "body_region": "pernas"},
            {"id": 4, "name": "Panturrilhas", "body_region": "pernas"},
            {"id": 5, "name": "Peitoral", "body_region": "superior"},
            {"id": 6, "name": "Deltoide anterior", "body_region": "superior"},
            {"id": 7, "name": "Deltoide lateral", "body_region": "superior"},
            {"id": 8, "name": "Dorsal", "body_region": "superior"},
            {"id": 9, "name": "Bíceps", "body_region": "superior"},
            {"id": 10, "name": "Tríceps", "body_region": "superior"},
            {"id": 11, "name": "Abdômen", "body_region": "core"},
            {"id": 12, "name": "Lombar", "body_region": "core"},
        ],
    )
    op.bulk_insert(
        exercises,
        [
            {"id": 1, "name": "Leg Press 45°", "slug": "leg-press-45", "description": "Exercício composto para membros inferiores.", "execution_instructions": "Posicione os pés na plataforma, controle a descida e empurre mantendo joelhos alinhados.", "difficulty": "beginner", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 2, "name": "Hack Machine", "slug": "hack-machine", "description": "Variação guiada de agachamento com foco em quadríceps.", "execution_instructions": "Mantenha o tronco apoiado, desça com controle e suba sem travar completamente os joelhos.", "difficulty": "intermediate", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 3, "name": "Agachamento Smith", "slug": "agachamento-smith", "description": "Agachamento guiado na máquina Smith.", "execution_instructions": "Ajuste a barra, mantenha coluna neutra e realize o movimento com amplitude controlada.", "difficulty": "intermediate", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 4, "name": "Bulgarian Split Squat", "slug": "bulgarian-split-squat", "description": "Exercício unilateral para pernas e glúteos.", "execution_instructions": "Apoie o pé traseiro no banco e flexione a perna da frente mantendo controle.", "difficulty": "advanced", "exercise_type": "strength", "is_unilateral": True, "is_compound": True},
            {"id": 5, "name": "Cadeira Extensora", "slug": "cadeira-extensora", "description": "Exercício isolado para quadríceps.", "execution_instructions": "Ajuste o banco, estenda os joelhos e controle a volta.", "difficulty": "beginner", "exercise_type": "strength", "is_unilateral": False, "is_compound": False},
            {"id": 6, "name": "Supino Reto com Barra", "slug": "supino-reto-barra", "description": "Exercício composto para peitoral.", "execution_instructions": "Controle a descida da barra até o peito e empurre mantendo escápulas estabilizadas.", "difficulty": "intermediate", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 7, "name": "Supino com Halteres", "slug": "supino-halteres", "description": "Variação com halteres para peitoral.", "execution_instructions": "Desça os halteres de forma controlada e empurre sem perder alinhamento.", "difficulty": "intermediate", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 8, "name": "Puxada na Polia", "slug": "puxada-polia", "description": "Exercício para dorsais usando polia alta.", "execution_instructions": "Puxe a barra em direção ao peitoral, mantendo tronco firme.", "difficulty": "beginner", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 9, "name": "Remada Curvada com Barra", "slug": "remada-curvada-barra", "description": "Exercício composto para costas.", "execution_instructions": "Incline o tronco, mantenha coluna neutra e puxe a barra em direção ao abdômen.", "difficulty": "intermediate", "exercise_type": "strength", "is_unilateral": False, "is_compound": True},
            {"id": 10, "name": "Prancha", "slug": "prancha", "description": "Exercício isométrico para core.", "execution_instructions": "Mantenha alinhamento entre ombros, quadril e tornozelos.", "difficulty": "beginner", "exercise_type": "strength", "is_unilateral": False, "is_compound": False},
        ],
    )
    op.bulk_insert(
        exercise_muscles,
        [
            {"id": 1, "exercise_id": 1, "muscle_group_id": 1, "role": "primary", "activation_level": 100},
            {"id": 2, "exercise_id": 1, "muscle_group_id": 2, "role": "secondary", "activation_level": 70},
            {"id": 3, "exercise_id": 2, "muscle_group_id": 1, "role": "primary", "activation_level": 100},
            {"id": 4, "exercise_id": 2, "muscle_group_id": 2, "role": "secondary", "activation_level": 60},
            {"id": 5, "exercise_id": 3, "muscle_group_id": 1, "role": "primary", "activation_level": 95},
            {"id": 6, "exercise_id": 3, "muscle_group_id": 2, "role": "secondary", "activation_level": 75},
            {"id": 7, "exercise_id": 4, "muscle_group_id": 1, "role": "primary", "activation_level": 90},
            {"id": 8, "exercise_id": 4, "muscle_group_id": 2, "role": "secondary", "activation_level": 80},
            {"id": 9, "exercise_id": 5, "muscle_group_id": 1, "role": "primary", "activation_level": 100},
            {"id": 10, "exercise_id": 6, "muscle_group_id": 5, "role": "primary", "activation_level": 100},
            {"id": 11, "exercise_id": 6, "muscle_group_id": 10, "role": "secondary", "activation_level": 65},
            {"id": 12, "exercise_id": 7, "muscle_group_id": 5, "role": "primary", "activation_level": 95},
            {"id": 13, "exercise_id": 8, "muscle_group_id": 8, "role": "primary", "activation_level": 100},
            {"id": 14, "exercise_id": 8, "muscle_group_id": 9, "role": "secondary", "activation_level": 60},
            {"id": 15, "exercise_id": 9, "muscle_group_id": 8, "role": "primary", "activation_level": 95},
            {"id": 16, "exercise_id": 10, "muscle_group_id": 11, "role": "primary", "activation_level": 100},
        ],
    )
    op.bulk_insert(
        exercise_equipment,
        [
            {"id": 1, "exercise_id": 1, "equipment_id": 4, "is_required": True},
            {"id": 2, "exercise_id": 2, "equipment_id": 5, "is_required": True},
            {"id": 3, "exercise_id": 3, "equipment_id": 3, "is_required": True},
            {"id": 4, "exercise_id": 4, "equipment_id": 1, "is_required": True},
            {"id": 5, "exercise_id": 4, "equipment_id": 6, "is_required": True},
            {"id": 6, "exercise_id": 5, "equipment_id": 9, "is_required": True},
            {"id": 7, "exercise_id": 6, "equipment_id": 2, "is_required": True},
            {"id": 8, "exercise_id": 6, "equipment_id": 6, "is_required": True},
            {"id": 9, "exercise_id": 7, "equipment_id": 1, "is_required": True},
            {"id": 10, "exercise_id": 7, "equipment_id": 6, "is_required": True},
            {"id": 11, "exercise_id": 8, "equipment_id": 7, "is_required": True},
            {"id": 12, "exercise_id": 9, "equipment_id": 2, "is_required": True},
            {"id": 13, "exercise_id": 10, "equipment_id": 8, "is_required": True},
        ],
    )
    op.bulk_insert(
        exercise_alternatives,
        [
            {"id": 1, "exercise_id": 1, "alternative_exercise_id": 2, "equivalence_score": 98, "reason": "Movimento guiado semelhante com alta ativação de quadríceps."},
            {"id": 2, "exercise_id": 1, "alternative_exercise_id": 3, "equivalence_score": 95, "reason": "Mantém padrão de agachamento guiado."},
            {"id": 3, "exercise_id": 1, "alternative_exercise_id": 4, "equivalence_score": 88, "reason": "Alternativa unilateral eficiente quando máquinas estão ocupadas."},
            {"id": 4, "exercise_id": 1, "alternative_exercise_id": 5, "equivalence_score": 82, "reason": "Foco em quadríceps, porém com menor equivalência por ser isolado."},
            {"id": 5, "exercise_id": 5, "alternative_exercise_id": 1, "equivalence_score": 80, "reason": "Alternativa composta para quadríceps."},
            {"id": 6, "exercise_id": 6, "alternative_exercise_id": 7, "equivalence_score": 90, "reason": "Mantém foco em peitoral com equipamento diferente."},
            {"id": 7, "exercise_id": 8, "alternative_exercise_id": 9, "equivalence_score": 70, "reason": "Mantém trabalho de dorsais, mas com padrão de remada."},
        ],
    )

    for table_name in [
        "equipment",
        "muscle_groups",
        "exercises",
        "exercise_muscles",
        "exercise_equipment",
        "exercise_alternatives",
    ]:
        op.execute(
            f"SELECT setval(pg_get_serial_sequence('{table_name}', 'id'), "
            f"COALESCE((SELECT MAX(id) FROM {table_name}), 1));"
        )


def downgrade() -> None:
    op.drop_index(op.f("ix_user_gym_equipment_id"), table_name="user_gym_equipment")
    op.drop_table("user_gym_equipment")
    op.drop_index(op.f("ix_exercise_alternatives_id"), table_name="exercise_alternatives")
    op.drop_table("exercise_alternatives")
    op.drop_index(op.f("ix_exercise_equipment_id"), table_name="exercise_equipment")
    op.drop_table("exercise_equipment")
    op.drop_index(op.f("ix_exercise_muscles_id"), table_name="exercise_muscles")
    op.drop_table("exercise_muscles")
    op.drop_index(op.f("ix_equipment_name"), table_name="equipment")
    op.drop_index(op.f("ix_equipment_id"), table_name="equipment")
    op.drop_table("equipment")
    op.drop_index(op.f("ix_muscle_groups_name"), table_name="muscle_groups")
    op.drop_index(op.f("ix_muscle_groups_id"), table_name="muscle_groups")
    op.drop_table("muscle_groups")
    op.drop_index(op.f("ix_exercises_slug"), table_name="exercises")
    op.drop_index(op.f("ix_exercises_id"), table_name="exercises")
    op.drop_table("exercises")
