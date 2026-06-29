# API v1 — Mo² LOG

Base URL local:

```text
http://localhost:8000/api/v1
```

## Endpoints implementados

```http
GET /health
GET /users
GET /exercises
GET /exercises/{id}
GET /exercises/{id}/alternatives
GET /equipment
GET /muscle-groups
GET /user-gym-equipment?user_id={id}
POST /user-gym-equipment
```

## Alternativas de exercícios

```http
GET /exercises/1/alternatives?mode=default
GET /exercises/1/alternatives?mode=all
```

`mode=default` remove sugestões que dependem de equipamentos marcados como indisponíveis para o usuário.

`mode=all` retorna todas as opções, incluindo as indisponíveis, com aviso em `equipment_status`.


## Analytics

```http
GET /api/v1/goals?user_id=1
POST /api/v1/goals
PATCH /api/v1/goals/{goal_id}/progress
GET /api/v1/personal-records?user_id=1
GET /api/v1/statistics/week?user_id=1&reference_date=2026-06-29
```
