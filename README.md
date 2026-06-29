# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

## Versão atual

`v1.2.0`

## Rodando localmente

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
```

Frontend:

```text
http://localhost:5173
```

Backend:

```text
http://localhost:8000/api/v1/health
http://localhost:8000/docs
```

## Strava OAuth

Para usar a integração real com Strava, configure no `backend/.env`:

```env
STRAVA_CLIENT_ID=
STRAVA_CLIENT_SECRET=
STRAVA_REDIRECT_URI=http://localhost:8000/api/v1/auth/strava/callback
STRAVA_SCOPE=read,activity:read
```

Sem essas credenciais, o endpoint de sincronização mantém o modo mock para desenvolvimento local.


## v1.2.0 - Running Sources

O Mo² LOG deixa de depender exclusivamente do Strava para corridas.

Fontes suportadas no MVP:

- Cadastro manual de corrida na esteira.
- Cadastro manual de corrida externa via API.
- Sincronização Strava opcional.
- Mock Strava para desenvolvimento local.

Fontes planejadas:

- Samsung Health por exportação de dados.
- Health Connect em versão mobile futura.
- Importação GPX/CSV/FIT.
