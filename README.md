# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

O objetivo é centralizar planejamento, execução, adaptação e evolução dos treinos.

## Status atual

Release: `v0.4.0`

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
```

## Regra de equipamento indisponível

Quando um equipamento é marcado como `unavailable`, os exercícios dependentes dele deixam de aparecer nas sugestões padrão, mas continuam disponíveis em `mode=all`.

Exemplo:

```text
GET /api/v1/exercises/1/alternatives?mode=all&user_id=1
```
