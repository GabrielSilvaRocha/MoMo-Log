from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
from app.models.exercise import Exercise


class WorkoutTemplate(Base):
    __tablename__ = "workout_templates"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(160), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    goal: Mapped[str | None] = mapped_column(String(120), nullable=True)
    difficulty: Mapped[str] = mapped_column(String(40), nullable=False, default="intermediate")
    estimated_duration_minutes: Mapped[int | None] = mapped_column(Integer, nullable=True)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="active", index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    exercises: Mapped[list["WorkoutTemplateExercise"]] = relationship(
        back_populates="template",
        cascade="all, delete-orphan",
        order_by="WorkoutTemplateExercise.order_index",
    )


class WorkoutTemplateExercise(Base):
    __tablename__ = "workout_template_exercises"
    __table_args__ = (
        UniqueConstraint("workout_template_id", "order_index", name="uq_workout_template_exercise_order"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    workout_template_id: Mapped[int] = mapped_column(
        ForeignKey("workout_templates.id", ondelete="CASCADE"), nullable=False, index=True
    )
    exercise_id: Mapped[int] = mapped_column(ForeignKey("exercises.id", ondelete="RESTRICT"), nullable=False)
    order_index: Mapped[int] = mapped_column(Integer, nullable=False)
    planned_sets: Mapped[int] = mapped_column(Integer, nullable=False, default=3)
    planned_reps: Mapped[str] = mapped_column(String(40), nullable=False, default="8-12")
    rest_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True, default=90)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    template: Mapped[WorkoutTemplate] = relationship(back_populates="exercises")
    exercise: Mapped[Exercise] = relationship()
