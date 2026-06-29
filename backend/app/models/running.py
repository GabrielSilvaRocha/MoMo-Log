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
