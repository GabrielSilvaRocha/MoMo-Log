# Mo² LOG — Release v1.5.0

Hybrid training app for strength training and running.

This release improves the workout execution experience with a set checklist, automatic rest timer, workout summary cards and a more practical flow for gym sessions.

## Highlights

- Visual checklist for planned strength sets.
- Automatic rest timer after logging a set.
- Manual rest timer per exercise.
- Registered volume summary during the workout.
- Average RPE summary during the workout.
- Adaptation Engine preserved for exercise swaps when equipment is busy or unavailable.

## Run

```bash
docker compose down
docker compose up --build
```

Then run migrations and tests:

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m pytest -q -vv
```

Frontend: http://localhost:5173
Backend health: http://localhost:8000/api/v1/health
