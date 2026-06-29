# Changelog

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
