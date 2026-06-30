# Mo² LOG

Mo² LOG é um aplicativo de treino híbrido para musculação e corrida.

## Versão atual

`v1.6.0` — Histórico de Treinos.

## Módulos disponíveis

- Dashboard semanal
- Planejamento editável
- Execução de treino de musculação
- Biblioteca de exercícios
- Minha academia e equipamentos disponíveis
- Motor de adaptação para troca de exercícios
- Corridas com fonte manual, esteira, mock Strava e base OAuth Strava
- Estatísticas, metas e recordes
- Histórico de sessões com filtros e detalhes

## Subir o projeto

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m pytest -q -vv
```

Frontend:

```text
http://localhost:5173
```

Backend:

```text
http://localhost:8000/api/v1/health
```

## Testes rápidos

```text
http://localhost:8000/api/v1/history/summary?user_id=1
http://localhost:8000/api/v1/history/sessions?user_id=1&limit=20
```
