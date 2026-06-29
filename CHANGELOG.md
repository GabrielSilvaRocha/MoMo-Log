# Changelog

## v0.6.0 - Running Core

### Added
- Running Activity model.
- Strava Account model.
- Strava Sync Log model.
- Alembic migration `20260629_0004_create_running_core`.
- Demo Strava account and running activity seed data.
- Endpoints:
  - `GET /api/v1/running-activities`
  - `GET /api/v1/running-activities/{id}`
  - `POST /api/v1/running-activities`
  - `GET /api/v1/strava/status`
  - `POST /api/v1/strava/sync`

### Business Rules
- Running activities can be linked to planned `TrainingSession` records.
- When a running activity is created for a planned running session, the session is marked as completed.
- Strava sync is currently mocked for local development and preserves the future API contract.
- Weekly dashboard now calculates running distance from `running_activities`.

### Changed
- API version updated to `0.6.0`.
- Training session response now includes linked running activity when available.

## v0.5.0 - Training Core

### Added
- Training Plan model.
- Training Session model as the central entity of the app.
- Strength workout exercise model.
- Strength set log model.
- Exercise swap log model.
- Alembic migration `20260629_0003_create_training_core`.
- Demo hybrid training plan seed data.
- Endpoints:
  - `GET /api/v1/training-plans/current`
  - `GET /api/v1/training-sessions/week`
  - `GET /api/v1/training-sessions/{id}`
  - `POST /api/v1/training-sessions`
  - `POST /api/v1/training-sessions/{id}/start`
  - `POST /api/v1/training-sessions/{id}/finish`
  - `POST /api/v1/training-sessions/{id}/reschedule`
  - `POST /api/v1/training-sessions/{id}/swap-exercise`
  - `POST /api/v1/strength/set-logs`
  - `GET /api/v1/dashboard/week`

### Business Rules
- `TrainingSession` is now the central entity for planned and executed workouts.
- Strength training execution stores sets, reps, load, RIR and RPE.
- Exercise swaps update the planned exercise and register the reason and equivalence score.
- Weekly dashboard returns completed sessions, today's sessions and upcoming sessions.

### Changed
- API version updated to `0.5.0`.

## v0.4.0 - Exercise Library Core

### Added
- Exercise Library domain models.
- Tables for exercises, muscle groups, equipment, exercise muscles, exercise equipment, alternatives, and user gym equipment.
- Initial seed data for common muscle groups, equipment and exercises.
- Endpoints:
  - `GET /api/v1/exercises`
  - `GET /api/v1/exercises/{id}`
  - `GET /api/v1/exercises/{id}/alternatives`
  - `GET /api/v1/equipment`
  - `GET /api/v1/muscle-groups`
  - `GET /api/v1/user-gym-equipment?user_id=1`
  - `POST /api/v1/user-gym-equipment`

### Changed
- API version updated to `0.4.0`.

### Business Rules
- Equipment marked as `unavailable` is removed from default exercise alternatives.
- Unavailable equipment still appears when `mode=all` is used in the alternatives endpoint.

## v0.3.0 - Backend Foundation

### Fixed
- Removed circular imports between Base, models and Alembic metadata.

### Added
- FastAPI backend foundation.
- PostgreSQL with Docker Compose.
- SQLAlchemy base and session.
- Alembic configured.
- User model and migration.
- Health endpoint.
- Users endpoint.
