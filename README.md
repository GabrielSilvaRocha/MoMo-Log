# Mo² LOG

**v6.0.0 — Templates e Workout Builder**

O **Mo² LOG** é uma aplicação de treino híbrido para musculação e corrida. O foco é ajudar o usuário a planejar, executar, adaptar e analisar treinos combinando musculação, corrida de rua/esteira, histórico, relatórios e inteligência inicial.

## Destaques da versão

- Backend FastAPI + PostgreSQL + SQLAlchemy + Alembic.
- Frontend React + Vite + TypeScript + Tailwind.
- Autenticação local com usuário demo sem senha versionada.
- Dashboard, planejamento, treino do dia, corridas, histórico, relatórios, inteligência e deploy readiness.
- Cadastro manual de corrida, com foco em esteira.
- Motor de adaptação para troca de exercícios quando a academia está cheia.
- Exportação CSV.
- **Novo:** templates de treino para criar sessões de musculação rapidamente.

## Rodando localmente

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
http://localhost:8000/docs
```

## Fluxo recomendado de teste

1. Entrar usando o botão **Entrar como Demo Local**.
2. Acessar **Templates**.
3. Selecionar um template.
4. Escolher uma data.
5. Clicar em **Criar sessão**.
6. Ir para **Planejamento** e validar a nova sessão criada.
7. Abrir **Treino do dia** quando a data corresponder.

## Principais endpoints novos

```text
GET  /api/v1/workout-templates?user_id=1
GET  /api/v1/workout-templates/{template_id}
POST /api/v1/workout-templates/{template_id}/schedule
```

## Segurança

- Não versionar `.env`.
- Não versionar senhas, tokens, client secrets ou chaves reais.
- Usar `.env.example` apenas com placeholders.
