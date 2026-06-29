# Mo² LOG

Mo² LOG is a hybrid training platform for strength training and running.

The product helps the user plan, execute, adapt and analyze workouts. Strength training is handled directly inside Mo² LOG, including exercise swaps when the gym is crowded. Running data is imported through Strava and analyzed together with the weekly plan.

## Current version

`v0.8.0 - Frontend Foundation`

## Stack

### Backend
- FastAPI
- SQLAlchemy
- Alembic
- PostgreSQL
- Docker

### Frontend
- React
- Vite
- TypeScript
- TailwindCSS
- Docker

## Running locally

```bash
docker compose down
docker compose up --build
```

In another terminal, apply migrations:

```bash
docker compose exec backend alembic upgrade head
```

## URLs

Backend health check:

```text
http://localhost:8000/api/v1/health
```

Backend docs:

```text
http://localhost:8000/docs
```

Frontend:

```text
http://localhost:5173
```

## Main test endpoints

```text
http://localhost:8000/api/v1/dashboard/week?user_id=1&reference_date=2026-06-29
http://localhost:8000/api/v1/statistics/week?user_id=1&reference_date=2026-06-29
http://localhost:8000/api/v1/goals?user_id=1
http://localhost:8000/api/v1/strava/status?user_id=1
```

## Product direction

Mo² LOG is not just a running tracker. It is a hybrid training operating system:

- Strength workout planning
- Exercise execution logging
- Exercise substitution when equipment is busy or unavailable
- User gym equipment preferences
- Running import through Strava
- Weekly dashboard with completed and upcoming sessions
- Goals, personal records and insights
