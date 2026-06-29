from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
from app.models.exercise import Exercise


class TrainingPlan(Base):
    __tablename__ = "training_plans"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(160), nullable=False)
    goal: Mapped[str | None] = mapped_column(String(160), nullable=True)
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    end_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="active")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    sessions: Mapped[list["TrainingSession"]] = relationship(
        back_populates="training_plan",
        cascade="all, delete-orphan",
    )


class TrainingSession(Base):
    __tablename__ = "training_sessions"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    training_plan_id: Mapped[int | None] = mapped_column(
        ForeignKey("training_plans.id", ondelete="SET NULL"), nullable=True, index=True
    )
    session_type: Mapped[str] = mapped_column(String(40), nullable=False)
    title: Mapped[str] = mapped_column(String(160), nullable=False)
    scheduled_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="planned", index=True)
    source: Mapped[str] = mapped_column(String(40), nullable=False, default="manual")
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    training_plan: Mapped[TrainingPlan | None] = relationship(back_populates="sessions")
    strength_exercises: Mapped[list["StrengthWorkoutExercise"]] = relationship(
        back_populates="training_session",
        cascade="all, delete-orphan",
        order_by="StrengthWorkoutExercise.order_index",
    )
    swap_logs: Mapped[list["ExerciseSwapLog"]] = relationship(
        back_populates="training_session",
        cascade="all, delete-orphan",
    )


class StrengthWorkoutExercise(Base):
    __tablename__ = "strength_workout_exercises"
    __table_args__ = (
        UniqueConstraint("training_session_id", "order_index", name="uq_strength_exercise_order"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    training_session_id: Mapped[int] = mapped_column(
        ForeignKey("training_sessions.id", ondelete="CASCADE"), nullable=False, index=True
    )
    exercise_id: Mapped[int] = mapped_column(ForeignKey("exercises.id", ondelete="RESTRICT"), nullable=False)
    order_index: Mapped[int] = mapped_column(Integer, nullable=False)
    planned_sets: Mapped[int] = mapped_column(Integer, nullable=False, default=3)
    planned_reps: Mapped[str] = mapped_column(String(40), nullable=False, default="8-12")
    planned_load: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    rest_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    training_session: Mapped[TrainingSession] = relationship(back_populates="strength_exercises")
    exercise: Mapped[Exercise] = relationship()
    set_logs: Mapped[list["StrengthSetLog"]] = relationship(
        back_populates="strength_workout_exercise",
        cascade="all, delete-orphan",
        order_by="StrengthSetLog.set_number",
    )


class StrengthSetLog(Base):
    __tablename__ = "strength_set_logs"
    __table_args__ = (
        UniqueConstraint("strength_workout_exercise_id", "set_number", name="uq_strength_set_number"),
        CheckConstraint("reps >= 0", name="ck_strength_set_reps_positive"),
        CheckConstraint("load >= 0", name="ck_strength_set_load_positive"),
        CheckConstraint("rir IS NULL OR (rir >= 0 AND rir <= 10)", name="ck_strength_set_rir_range"),
        CheckConstraint("rpe IS NULL OR (rpe >= 0 AND rpe <= 10)", name="ck_strength_set_rpe_range"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    strength_workout_exercise_id: Mapped[int] = mapped_column(
        ForeignKey("strength_workout_exercises.id", ondelete="CASCADE"), nullable=False, index=True
    )
    set_number: Mapped[int] = mapped_column(Integer, nullable=False)
    reps: Mapped[int] = mapped_column(Integer, nullable=False)
    load: Mapped[Decimal] = mapped_column(Numeric(8, 2), nullable=False, default=0)
    rir: Mapped[int | None] = mapped_column(Integer, nullable=True)
    rpe: Mapped[int | None] = mapped_column(Integer, nullable=True)
    completed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    strength_workout_exercise: Mapped[StrengthWorkoutExercise] = relationship(back_populates="set_logs")


class ExerciseSwapLog(Base):
    __tablename__ = "exercise_swap_logs"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    training_session_id: Mapped[int] = mapped_column(
        ForeignKey("training_sessions.id", ondelete="CASCADE"), nullable=False, index=True
    )
    original_exercise_id: Mapped[int] = mapped_column(ForeignKey("exercises.id", ondelete="RESTRICT"), nullable=False)
    new_exercise_id: Mapped[int] = mapped_column(ForeignKey("exercises.id", ondelete="RESTRICT"), nullable=False)
    reason: Mapped[str] = mapped_column(String(80), nullable=False)
    equivalence_score: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    training_session: Mapped[TrainingSession] = relationship(back_populates="swap_logs")
