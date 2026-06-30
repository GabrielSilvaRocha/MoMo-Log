# Mo² LOG

**Mo² LOG** é um aplicativo de treino híbrido para musculação e corrida. O foco do produto é ajudar o usuário a planejar, executar, adaptar e acompanhar sua evolução semanal.

## Status

Versão atual: **v2.0.0 — MVP Consolidado**

## Módulos do MVP

- Dashboard semanal
- Planejamento editável
- Execução de musculação com séries, carga, RIR, RPE e descanso
- Motor de adaptação para trocar exercícios quando a academia estiver cheia
- Configuração de equipamentos da academia
- Corridas com cadastro manual, foco em esteira, e Strava opcional
- Histórico de treinos
- Relatórios e exportação CSV
- Metas, recordes e estatísticas iniciais

## Stack

### Backend
- FastAPI
- SQLAlchemy
- Alembic
- PostgreSQL
- Pytest

### Frontend
- React
- Vite
- TypeScript
- TailwindCSS

### Infra
- Docker Compose
- GitHub Actions

## Como rodar

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
Backend:  http://localhost:8000/docs
Health:   http://localhost:8000/api/v1/health
MVP:      http://localhost:8000/api/v1/product/mvp-status
```

## Comandos úteis

```bash
make rebuild
make migrate
make test
make logs
```

## Decisão de produto

O Mo² LOG não depende exclusivamente do Strava. Como muitas corridas serão na esteira, o cadastro manual é parte central do Running Core. O Strava permanece como integração opcional.

## Próximas milestones

- v3.0.0 — autenticação real, perfil e dados separados por usuário
- v4.0.0 — inteligência, comparação planejado vs realizado e insights avançados
- v5.0.0 — deploy, documentação de portfólio e versão demonstrável
