# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

O objetivo do produto é centralizar planejamento, execução, adaptação e evolução dos treinos. A corrida usa o Strava como fonte oficial de dados, enquanto a musculação possui controle próprio de exercícios, séries, cargas e substituições inteligentes.

## Stack inicial

- FastAPI
- PostgreSQL
- SQLAlchemy
- Alembic
- Docker Compose
- Pytest

## Rodar localmente

```bash
docker compose up --build
```

Teste:

```text
http://localhost:8000/api/v1/health
```

## Estrutura

```text
backend/
docs/
.github/
docker-compose.yml
```
