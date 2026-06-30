"""create running coach core

Revision ID: 20260630_0009
Revises: 20260630_0008
Create Date: 2026-06-30
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20260630_0009"
down_revision: Union[str, None] = "20260630_0008"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "running_goals",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("goal_type", sa.String(length=40), nullable=False, server_default="race"),
        sa.Column("race_distance_km", sa.Numeric(6, 2), nullable=False),
        sa.Column("race_date", sa.DateTime(timezone=True), nullable=False, index=True),
        sa.Column("current_5k_time_seconds", sa.Integer(), nullable=True),
        sa.Column("target_5k_time_seconds", sa.Integer(), nullable=True),
        sa.Column("training_location", sa.String(length=40), nullable=False, server_default="treadmill"),
        sa.Column("available_weekdays", sa.String(length=40), nullable=False, server_default="mon,tue,wed,thu,fri"),
        sa.Column("status", sa.String(length=40), nullable=False, server_default="active"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_table(
        "running_plan_sessions",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("goal_id", sa.Integer(), sa.ForeignKey("running_goals.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("training_session_id", sa.Integer(), sa.ForeignKey("training_sessions.id", ondelete="SET NULL"), nullable=True, index=True),
        sa.Column("session_type", sa.String(length=40), nullable=False),
        sa.Column("title", sa.String(length=180), nullable=False),
        sa.Column("scheduled_date", sa.DateTime(timezone=True), nullable=False, index=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("target_distance_km", sa.Numeric(6, 2), nullable=True),
        sa.Column("target_duration_seconds", sa.Integer(), nullable=True),
        sa.Column("target_pace_seconds_per_km", sa.Integer(), nullable=True),
        sa.Column("target_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False, server_default="planned"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_table(
        "running_workout_steps",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("running_plan_session_id", sa.Integer(), sa.ForeignKey("running_plan_sessions.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("order_index", sa.Integer(), nullable=False),
        sa.Column("step_type", sa.String(length=40), nullable=False),
        sa.Column("title", sa.String(length=120), nullable=False),
        sa.Column("target_distance_m", sa.Integer(), nullable=True),
        sa.Column("target_duration_seconds", sa.Integer(), nullable=True),
        sa.Column("target_pace_seconds_per_km", sa.Integer(), nullable=True),
        sa.Column("target_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("rest_seconds", sa.Integer(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
    )
    op.create_table(
        "running_execution_logs",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("running_plan_session_id", sa.Integer(), sa.ForeignKey("running_plan_sessions.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("finished_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("status", sa.String(length=40), nullable=False, server_default="in_progress"),
        sa.Column("total_distance_km", sa.Numeric(6, 2), nullable=True),
        sa.Column("total_duration_seconds", sa.Integer(), nullable=True),
        sa.Column("average_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("average_pace_seconds_per_km", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_table(
        "running_step_logs",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("running_execution_log_id", sa.Integer(), sa.ForeignKey("running_execution_logs.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("running_workout_step_id", sa.Integer(), sa.ForeignKey("running_workout_steps.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("planned_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("actual_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("finished_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("completed", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.create_table(
        "running_speed_adjustments",
        sa.Column("id", sa.Integer(), primary_key=True, index=True),
        sa.Column("running_step_log_id", sa.Integer(), sa.ForeignKey("running_step_logs.id", ondelete="CASCADE"), nullable=False, index=True),
        sa.Column("adjustment_type", sa.String(length=30), nullable=False),
        sa.Column("previous_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("new_speed_kmh", sa.Numeric(5, 2), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )

    # Goal inicial do Gabriel: prova 5 km em 16/08/2026, referência atual 24:58.
    op.execute("""
        INSERT INTO running_goals (
            id, user_id, goal_type, race_distance_km, race_date,
            current_5k_time_seconds, target_5k_time_seconds, training_location,
            available_weekdays, status
        ) VALUES (
            1, 1, 'race', 5.00, '2026-08-16 09:00:00+00',
            1498, 1410, 'treadmill', 'mon,tue,wed,thu,fri', 'active'
        ) ON CONFLICT DO NOTHING;
    """)
    op.execute("""
        INSERT INTO running_plan_sessions (
            id, user_id, goal_id, session_type, title, scheduled_date, description,
            target_distance_km, target_duration_seconds, target_pace_seconds_per_km, target_speed_kmh, status
        ) VALUES
            (1, 1, 1, 'easy', 'Rodagem leve na esteira', '2026-06-29 19:00:00+00', 'Base aeróbica controlada. Mantenha confortável.', 5.00, 1650, 330, 10.91, 'planned'),
            (2, 1, 1, 'interval', 'Intervalado 6x400m', '2026-07-01 19:00:00+00', 'Tiros de 400m com recuperação ativa.', 5.80, 2100, 300, 12.00, 'planned'),
            (3, 1, 1, 'tempo', 'Tempo run 3 km', '2026-07-03 19:00:00+00', 'Trecho contínuo em ritmo forte controlado.', 6.00, 1980, 315, 11.43, 'planned')
        ON CONFLICT DO NOTHING;
    """)
    op.execute("""
        INSERT INTO running_workout_steps (
            running_plan_session_id, order_index, step_type, title, target_distance_m, target_duration_seconds,
            target_pace_seconds_per_km, target_speed_kmh, rest_seconds, notes
        ) VALUES
            (1, 1, 'warmup', 'Aquecimento', NULL, 300, 390, 9.23, NULL, 'Caminhada rápida/trote leve'),
            (1, 2, 'run', 'Rodagem leve', 5000, 1650, 330, 10.91, NULL, 'Controle respiratório e constância'),
            (1, 3, 'cooldown', 'Desaquecimento', NULL, 240, 420, 8.57, NULL, 'Reduza gradualmente'),
            (2, 1, 'warmup', 'Aquecimento', NULL, 600, 390, 9.23, NULL, 'Prepare para os tiros'),
            (2, 2, 'interval', 'Tiro 1', 400, 110, 275, 13.09, NULL, 'Ritmo de 5 km forte'),
            (2, 3, 'recovery', 'Recuperação 1', NULL, 90, 600, 6.00, 90, 'Recuperação ativa'),
            (2, 4, 'interval', 'Tiro 2', 400, 110, 275, 13.09, NULL, 'Repita o ritmo'),
            (2, 5, 'recovery', 'Recuperação 2', NULL, 90, 600, 6.00, 90, 'Respire e ajuste'),
            (2, 6, 'interval', 'Tiro 3', 400, 110, 275, 13.09, NULL, 'Controle postura'),
            (2, 7, 'recovery', 'Recuperação 3', NULL, 90, 600, 6.00, 90, 'Recuperação ativa'),
            (2, 8, 'interval', 'Tiro 4', 400, 110, 275, 13.09, NULL, 'Mantenha cadência'),
            (2, 9, 'recovery', 'Recuperação 4', NULL, 90, 600, 6.00, 90, 'Recuperação ativa'),
            (2, 10, 'interval', 'Tiro 5', 400, 110, 275, 13.09, NULL, 'Penúltimo tiro'),
            (2, 11, 'recovery', 'Recuperação 5', NULL, 90, 600, 6.00, 90, 'Recuperação ativa'),
            (2, 12, 'interval', 'Tiro 6', 400, 110, 275, 13.09, NULL, 'Último tiro'),
            (2, 13, 'cooldown', 'Desaquecimento', NULL, 480, 420, 8.57, NULL, 'Finalização leve'),
            (3, 1, 'warmup', 'Aquecimento', NULL, 600, 390, 9.23, NULL, 'Trote leve'),
            (3, 2, 'run', 'Bloco tempo', 3000, 945, 315, 11.43, NULL, 'Ritmo forte sustentável'),
            (3, 3, 'cooldown', 'Desaquecimento', NULL, 420, 420, 8.57, NULL, 'Solte as pernas')
        ON CONFLICT DO NOTHING;
    """)


def downgrade() -> None:
    op.drop_table("running_speed_adjustments")
    op.drop_table("running_step_logs")
    op.drop_table("running_execution_logs")
    op.drop_table("running_workout_steps")
    op.drop_table("running_plan_sessions")
    op.drop_table("running_goals")
