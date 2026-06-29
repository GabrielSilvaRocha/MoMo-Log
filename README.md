# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

O objetivo é centralizar planejamento, execução, adaptação e evolução dos treinos.

## Status atual

Release: `v0.5.0`

Inclui:

- FastAPI
- PostgreSQL
- SQLAlchemy
- Alembic
- Docker Compose
- Health Check
- Model `User`
- Biblioteca inicial de exercícios
- Equipamentos e grupos musculares
- Alternativas de exercícios
- Regra de equipamento indisponível por usuário
- Planejamento semanal
- Sessões de treino
- Exercícios planejados de musculação
- Registro de séries, carga, RIR e RPE
- Troca de exercício com histórico
- Dashboard semanal com concluídos, hoje e próximos treinos

## Rodar localmente

```bash
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
```

## Testes manuais

```text
http://localhost:8000/api/v1/health
http://localhost:8000/api/v1/users
http://localhost:8000/api/v1/exercises
http://localhost:8000/api/v1/equipment
http://localhost:8000/api/v1/muscle-groups
http://localhost:8000/api/v1/exercises/1/alternatives
http://localhost:8000/api/v1/training-plans/current?user_id=1
http://localhost:8000/api/v1/training-sessions/week?user_id=1&reference_date=2026-06-29
http://localhost:8000/api/v1/training-sessions/1
http://localhost:8000/api/v1/dashboard/week?user_id=1&reference_date=2026-06-29
```

## Fluxo manual de treino

Iniciar sessão:

```bash
curl -X POST http://localhost:8000/api/v1/training-sessions/1/start
```

Registrar uma série:

```bash
curl -X POST http://localhost:8000/api/v1/strength/set-logs \
  -H "Content-Type: application/json" \
  -d '{"strength_workout_exercise_id":1,"set_number":1,"reps":10,"load":120,"rir":2,"rpe":8}'
```

Trocar exercício:

```bash
curl -X POST http://localhost:8000/api/v1/training-sessions/1/swap-exercise \
  -H "Content-Type: application/json" \
  -d '{"strength_workout_exercise_id":1,"original_exercise_id":1,"new_exercise_id":2,"reason":"equipment_busy"}'
```

Finalizar sessão:

```bash
curl -X POST http://localhost:8000/api/v1/training-sessions/1/finish
```

## Regra de equipamento indisponível

Quando um equipamento é marcado como `unavailable`, os exercícios dependentes dele deixam de aparecer nas sugestões padrão, mas continuam disponíveis em `mode=all`.

Exemplo:

```text
GET /api/v1/exercises/1/alternatives?mode=all&user_id=1
```
