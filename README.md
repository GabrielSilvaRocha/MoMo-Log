# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

## Backend Foundation

Stack inicial:

- FastAPI
- PostgreSQL
- SQLAlchemy
- Alembic em etapa futura
- Docker Compose

## Como rodar

```bash
docker compose up --build
```

Health check:

```text
http://localhost:8000/api/v1/health
```

Resposta esperada:

```json
{
  "status": "ok",
  "app": "Mo² LOG",
  "version": "1.0.0",
  "environment": "development"
}
```
