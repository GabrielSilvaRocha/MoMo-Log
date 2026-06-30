# Mo² LOG

Mo² LOG é um aplicativo de treino híbrido para musculação e corrida. O produto combina planejamento semanal, execução de treinos, troca inteligente de exercícios quando a academia está cheia, registro manual de corridas na esteira, histórico, relatórios e evolução.

## Release atual

**v4.0.0 — Usuários, autenticação e preferências**

Esta versão consolida o MVP com uma camada inicial de usuário real:

- Cadastro de usuário.
- Login com token assinado.
- Login demo para acessar os dados populados.
- Tela de perfil.
- Preferências persistentes do usuário.
- Fonte padrão de corrida configurável, com `manual_treadmill` como padrão.
- Metas semanais de corrida e musculação por usuário.

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
http://localhost:5173
```

## Login demo

Use o botão **Entrar como Demo Local** ou informe:

```text
Use o botão **Entrar como Demo Local** em ambiente local. Nenhuma senha demo é versionada no repositório.
```

## URLs úteis

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8000
Swagger:  http://localhost:8000/docs
Health:   http://localhost:8000/api/v1/health
```

## Stack

- Backend: FastAPI, SQLAlchemy, Alembic, PostgreSQL.
- Frontend: React, Vite, TypeScript, TailwindCSS.
- Infra: Docker Compose.
- CI: GitHub Actions.

## Próximos marcos

- v4.0.0 — Importação GPX/CSV/FIT e melhoria de fontes de corrida.
- v5.0.0 — Intelligence Core: planejado vs realizado, tendências e insights.
- v6.0.0 — Deploy, portfólio e documentação final.


## v4.0.0 — Intelligence Core

Esta versão adiciona a primeira camada de inteligência do Mo² LOG: score híbrido semanal, insights acionáveis, comparação planejado vs realizado e previsão simples de 5 km.
