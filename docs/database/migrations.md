# Migrations

Rodar migrations:

```bash
docker compose exec backend alembic upgrade head
```

Criar nova migration futuramente:

```bash
docker compose exec backend alembic revision --autogenerate -m "message"
```
