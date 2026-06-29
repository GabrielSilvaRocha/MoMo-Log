# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

O objetivo é centralizar planejamento, execução, adaptação e evolução dos treinos.

## Status atual

Release: `v0.7.0`

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
- Core de corrida
- Atividades de corrida vinculadas a sessões planejadas
- Conta Strava simulada
- Sincronização Strava mock para desenvolvimento local

- Metas do usuário
- Recordes pessoais
- Estatísticas semanais consolidadas
- Insights iniciais de consistência, volume e corrida

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
http://localhost:8000/api/v1/running-activities?user_id=1
http://localhost:8000/api/v1/running-activities/1
http://localhost:8000/api/v1/strava/status?user_id=1
http://localhost:8000/api/v1/dashboard/week?user_id=1&reference_date=2026-06-29
http://localhost:8000/api/v1/goals?user_id=1
http://localhost:8000/api/v1/personal-records?user_id=1
http://localhost:8000/api/v1/statistics/week?user_id=1&reference_date=2026-06-29
```

## Sincronização Strava mock

A integração OAuth real será implementada em release futura. Nesta release, o endpoint abaixo simula a sincronização e cria atividades de corrida para sessões planejadas sem atividade vinculada:

```bash
curl -X POST "http://localhost:8000/api/v1/strava/sync?user_id=1"
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

## Registro manual de corrida

```bash
curl -X POST http://localhost:8000/api/v1/running-activities \
  -H "Content-Type: application/json" \
  -d '{"user_id":1,"training_session_id":4,"name":"Intervalado curto","distance_m":6000,"moving_time_s":2100,"elapsed_time_s":2160,"total_elevation_gain":35,"start_date":"2026-07-02T10:00:00Z"}'
```

## Regra de equipamento indisponível

Quando um equipamento é marcado como `unavailable`, os exercícios dependentes dele deixam de aparecer nas sugestões padrão, mas continuam disponíveis em `mode=all`.

Exemplo:

```text
GET /api/v1/exercises/1/alternatives?mode=all&user_id=1
```

## Metas e estatísticas

Listar metas:

```text
GET /api/v1/goals?user_id=1
```

Atualizar progresso de uma meta:

```bash
curl -X PATCH http://localhost:8000/api/v1/goals/1/progress \
  -H "Content-Type: application/json" \
  -d '{"current_value":12}'
```

Resumo semanal:

```text
GET /api/v1/statistics/week?user_id=1&reference_date=2026-06-29
```
