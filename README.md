# Mo² LOG

Mo² LOG é um aplicativo de treino híbrido para musculação e corrida.

O produto nasceu com dois objetivos principais:

1. Mostrar e registrar treinos de musculação, permitindo trocar exercícios quando a academia estiver cheia ou quando a máquina não existir.
2. Registrar corridas usando o Strava como fonte oficial dos dados de corrida.

## Status atual

Versão: `1.0.0`

Esta versão entrega o primeiro MVP navegável:

- Backend FastAPI
- PostgreSQL
- SQLAlchemy
- Alembic
- Docker Compose
- Biblioteca de exercícios
- Training Core
- Running Core mock
- Analytics Core
- Frontend React + Vite + TypeScript + TailwindCSS
- Dashboard
- Tela de treino do dia
- Tela de corridas
- Tela de estatísticas
- Tela de metas
- Tela de exercícios e equipamentos da academia

## Como executar

Na raiz do projeto:

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
```

Acesse:

- API: http://localhost:8000/api/v1/health
- Swagger: http://localhost:8000/docs
- Frontend: http://localhost:5173

## Testes do backend

```bash
docker compose exec backend python -m pytest -q -vv
```

## Fluxo principal do MVP

1. Abrir o Dashboard.
2. Ver o treino do dia, próximos treinos e metas.
3. Acessar Treino do dia.
4. Registrar séries.
5. Trocar exercício quando necessário.
6. Acessar Exercícios e marcar equipamento como indisponível, favorito ou frequentemente ocupado.
7. Acessar Corridas e sincronizar Strava mock.
8. Acessar Estatísticas para ver consistência, volume, quilometragem, metas e insights.

## Observação sobre Strava

A integração com Strava nesta versão ainda é simulada. A próxima etapa será preparar o OAuth real usando credenciais de uma aplicação Strava.
