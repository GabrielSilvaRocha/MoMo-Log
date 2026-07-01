# Deploy do Mo² LOG

Este documento descreve o caminho recomendado para publicar o Mo² LOG como demonstração de portfólio.

## Estratégia recomendada

- Frontend: Vercel, Netlify ou container Nginx.
- Backend: Railway, Render, Fly.io ou container próprio.
- Banco: PostgreSQL gerenciado.
- Variáveis: configuradas no provedor, nunca versionadas.

## Variáveis obrigatórias

```env
DATABASE_URL=
FRONTEND_ORIGIN=
SECRET_KEY=
POSTGRES_PASSWORD=
```

## Docker Compose de produção

Use o arquivo `docker-compose.prod.yml` como base:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up --build -d
```

Depois aplique migrations:

```bash
docker compose -f docker-compose.prod.yml exec backend alembic upgrade head
```

## Verificações pós-deploy

```text
GET /api/v1/health
GET /api/v1/ops/status
GET /api/v1/ops/deployment-checklist
```

## Observações

O fluxo de corrida não depende de integrações externas. O Running Coach gera plano e execução guiada para esteira.


## Cloud demo v7.1

Consulte docs/deployment/cloud-demo.md para publicar uma demonstração pública com frontend, backend e PostgreSQL gerenciado.

Manifestos de exemplo:

- infra/render-blueprint.example.yaml
- infra/vercel.frontend.example.json
