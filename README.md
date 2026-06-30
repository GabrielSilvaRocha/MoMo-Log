# Mo² LOG

Mo² LOG é um aplicativo de treino híbrido para musculação e corrida. O produto combina planejamento semanal, execução de treinos, troca inteligente de exercícios quando a academia está cheia, registro manual de corridas na esteira, histórico, relatórios, inteligência de evolução e preparação para deploy.

## Release atual

**v5.0.0 — Deploy, portfólio e qualidade operacional**

Esta versão consolida o projeto para apresentação técnica:

- Nova tela **Deploy**.
- Endpoints operacionais `/ops/status` e `/ops/deployment-checklist`.
- Docker Compose de produção base.
- Dockerfiles de produção para backend e frontend.
- Documentação de deploy, portfólio e política de secrets.
- README e changelog consolidados.
- Strava permanece opcional; cadastro manual de esteira continua sendo o fluxo principal para corrida.

## Como rodar localmente

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m pytest -q -vv
```

Acesse:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8000
Swagger:  http://localhost:8000/docs
Health:   http://localhost:8000/api/v1/health
Ops:      http://localhost:8000/api/v1/ops/status
```

## Login local

Use o botão **Entrar como Demo Local**. Nenhuma senha demo é versionada no repositório.

## Stack

- Backend: FastAPI, SQLAlchemy, Alembic, PostgreSQL.
- Frontend: React, Vite, TypeScript, TailwindCSS.
- Infra: Docker Compose.
- CI: GitHub Actions.
- Produto: planejamento híbrido, execução, adaptação, corrida manual, relatórios e inteligência.

## Deploy base

Copie `.env.production.example` para `.env.production`, preencha os valores reais fora do Git e execute:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up --build -d
```

Depois aplique migrations:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production exec backend alembic upgrade head
```

## Segurança

Não versionar:

- `.env` real
- tokens
- client secrets
- senhas
- chaves JWT reais

Use apenas `.env.example` e `.env.production.example` com placeholders.

## Próximos marcos

- v6.0.0 — Importação GPX/CSV/FIT.
- v7.0.0 — Evolução de carga por exercício.
- v8.0.0 — Mobile/Health Connect para Samsung Health.
