# API v1 — Mo² LOG

Base URL local:

```text
http://localhost:8000/api/v1
```

## Health

```http
GET /health
```

## Produto

```http
GET /product/mvp-status
GET /product/release-notes
```

## Dashboard

```http
GET /dashboard/week
```

## Planejamento e treino

```http
GET /training-plans/current
GET /training-sessions/week
GET /training-sessions/{id}
POST /training-sessions
PATCH /training-sessions/{id}
DELETE /training-sessions/{id}
POST /training-sessions/{id}/start
POST /training-sessions/{id}/finish
POST /training-sessions/{id}/strength-exercises
POST /strength/set-logs
POST /training-sessions/{id}/swap-exercise
```

## Corridas

```http
GET /running-activities
POST /running-activities
GET /running-activities/{id}
POST /strava/sync
GET /strava/status
GET /auth/strava/authorize
GET /auth/strava/callback
```

## Exercícios e academia

```http
GET /exercises
GET /exercises/{id}
GET /exercises/{id}/alternatives
GET /equipment
GET /muscle-groups
GET /user-gym-equipment
POST /user-gym-equipment
GET /adaptation/exercises/{exercise_id}/suggestions
```

## Analytics

```http
GET /goals
PATCH /goals/{goal_id}/progress
GET /personal-records
GET /statistics/week
```

## Histórico e relatórios

```http
GET /history/summary
GET /history/sessions
GET /reports/overview
GET /reports/export/sessions.csv
GET /reports/export/running.csv
GET /reports/export/strength.csv
```
