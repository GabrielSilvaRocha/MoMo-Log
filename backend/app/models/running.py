from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, ForeignKey, Integer, Numeric, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base


class StravaAccount(Base):
    __tablename__ = "strava_accounts"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    strava_athlete_id: Mapped[str] = mapped_column(String(80), unique=True, nullable=False, index=True)
    access_token_encrypted: Mapped[str | None] = mapped_column(Text, nullable=True)
    refresh_token_encrypted: Mapped[str | None] = mapped_column(Text, nullable=True)
    token_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    activities: Mapped[list["RunningActivity"]] = relationship(back_populates="strava_account")


class RunningActivity(Base):
    __tablename__ = "running_activities"
    __table_args__ = (
        UniqueConstraint("training_session_id", name="uq_running_activity_training_session"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    training_session_id: Mapped[int | None] = mapped_column(
        ForeignKey("training_sessions.id", ondelete="SET NULL"), nullable=True, index=True
    )
    strava_account_id: Mapped[int | None] = mapped_column(
        ForeignKey("strava_accounts.id", ondelete="SET NULL"), nullable=True, index=True
    )
    strava_activity_id: Mapped[str | None] = mapped_column(String(80), unique=True, nullable=True, index=True)
    name: Mapped[str] = mapped_column(String(180), nullable=False)
    distance_m: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    moving_time_s: Mapped[int] = mapped_column(Integer, nullable=False)
    elapsed_time_s: Mapped[int] = mapped_column(Integer, nullable=False)
    average_speed: Mapped[Decimal | None] = mapped_column(Numeric(10, 4), nullable=True)
    average_pace: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    max_speed: Mapped[Decimal | None] = mapped_column(Numeric(10, 4), nullable=True)
    total_elevation_gain: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    activity_type: Mapped[str] = mapped_column(String(40), nullable=False, default="Run")
    source: Mapped[str] = mapped_column(String(40), nullable=False, default="strava")
    start_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    raw_payload: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )

    strava_account: Mapped[StravaAccount | None] = relationship(back_populates="activities")
    training_session = relationship("TrainingSession", back_populates="running_activity")


class StravaSyncLog(Base):
    __tablename__ = "strava_sync_logs"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    imported_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    updated_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    ignored_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="success")
    message: Mapped[str | None] = mapped_column(Text, nullable=True)


class RunningGoal(Base):
    __tablename__ = "running_goals"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    goal_type: Mapped[str] = mapped_column(String(40), nullable=False, default="race")
    race_distance_km: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    race_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    current_5k_time_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_5k_time_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    training_location: Mapped[str] = mapped_column(String(40), nullable=False, default="treadmill")
    available_weekdays: Mapped[str] = mapped_column(String(40), nullable=False, default="mon,tue,wed,thu,fri")
    weekly_sessions: Mapped[int] = mapped_column(Integer, nullable=False, default=3)
    progression_style: Mapped[str] = mapped_column(String(40), nullable=False, default="balanced")
    long_run_weekday: Mapped[str] = mapped_column(String(10), nullable=False, default="fri")
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="active")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    plan_sessions: Mapped[list["RunningPlanSession"]] = relationship(back_populates="goal")


class RunningPlanSession(Base):
    __tablename__ = "running_plan_sessions"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    goal_id: Mapped[int] = mapped_column(ForeignKey("running_goals.id", ondelete="CASCADE"), nullable=False, index=True)
    training_session_id: Mapped[int | None] = mapped_column(ForeignKey("training_sessions.id", ondelete="SET NULL"), nullable=True, index=True)
    session_type: Mapped[str] = mapped_column(String(40), nullable=False)
    title: Mapped[str] = mapped_column(String(180), nullable=False)
    scheduled_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    target_distance_km: Mapped[Decimal | None] = mapped_column(Numeric(6, 2), nullable=True)
    target_duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_pace_seconds_per_km: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="planned")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    goal: Mapped[RunningGoal] = relationship(back_populates="plan_sessions")
    steps: Mapped[list["RunningWorkoutStep"]] = relationship(back_populates="plan_session", order_by="RunningWorkoutStep.order_index")


class RunningWorkoutStep(Base):
    __tablename__ = "running_workout_steps"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    running_plan_session_id: Mapped[int] = mapped_column(ForeignKey("running_plan_sessions.id", ondelete="CASCADE"), nullable=False, index=True)
    order_index: Mapped[int] = mapped_column(Integer, nullable=False)
    step_type: Mapped[str] = mapped_column(String(40), nullable=False)
    title: Mapped[str] = mapped_column(String(120), nullable=False)
    target_distance_m: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_pace_seconds_per_km: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    rest_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)

    plan_session: Mapped[RunningPlanSession] = relationship(back_populates="steps")


class RunningExecutionLog(Base):
    __tablename__ = "running_execution_logs"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    running_plan_session_id: Mapped[int] = mapped_column(ForeignKey("running_plan_sessions.id", ondelete="CASCADE"), nullable=False, index=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    status: Mapped[str] = mapped_column(String(40), nullable=False, default="in_progress")
    total_distance_km: Mapped[Decimal | None] = mapped_column(Numeric(6, 2), nullable=True)
    total_duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    average_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    average_pace_seconds_per_km: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class RunningStepLog(Base):
    __tablename__ = "running_step_logs"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    running_execution_log_id: Mapped[int] = mapped_column(ForeignKey("running_execution_logs.id", ondelete="CASCADE"), nullable=False, index=True)
    running_workout_step_id: Mapped[int] = mapped_column(ForeignKey("running_workout_steps.id", ondelete="CASCADE"), nullable=False, index=True)
    planned_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    actual_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    completed: Mapped[bool] = mapped_column(default=False, nullable=False)


class RunningSpeedAdjustment(Base):
    __tablename__ = "running_speed_adjustments"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    running_step_log_id: Mapped[int] = mapped_column(ForeignKey("running_step_logs.id", ondelete="CASCADE"), nullable=False, index=True)
    adjustment_type: Mapped[str] = mapped_column(String(30), nullable=False)
    previous_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    new_speed_kmh: Mapped[Decimal | None] = mapped_column(Numeric(5, 2), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
