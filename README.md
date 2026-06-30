# Mo² LOG

Mo² LOG is a hybrid training app for strength training and running.

## v1.4.0

This release introduces the first version of the Adaptation Engine.

### Highlights

- Smart exercise replacement ranking.
- Context-aware suggestions based on user gym equipment.
- Unavailable equipment is hidden from default suggestions but remains visible in "show all" mode.
- Frequently busy equipment receives a score penalty.
- Favorite equipment receives a score bonus.
- New frontend page: **Adaptação**.

## Running locally

```bash
docker compose down
docker compose up --build
```

In another terminal:

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m pytest -q -vv
```

Frontend:

```text
http://localhost:5173
```

API docs:

```text
http://localhost:8000/docs
```
