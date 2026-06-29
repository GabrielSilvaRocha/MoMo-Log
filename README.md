# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida.

Ele nasceu para ajudar o usuário a executar o plano de treino, adaptar exercícios quando a academia está cheia ou não possui determinado equipamento, e registrar corridas usando o Strava como fonte oficial.

## Versão atual

`v0.9.1` — Workout Execution UI

## Stack

### Backend

- FastAPI
- SQLAlchemy
- Alembic
- PostgreSQL
- Docker

### Frontend

- React
- Vite
- TypeScript
- TailwindCSS

## Como rodar

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
```

## URLs

Backend:

```text
http://localhost:8000/api/v1/health
http://localhost:8000/docs
```

Frontend:

```text
http://localhost:5173
```

## Funcionalidades disponíveis

- Dashboard semanal.
- Treinos de hoje, concluídos e próximos.
- Registro de volume de musculação.
- Corridas mock via Strava.
- Metas e estatísticas semanais.
- Execução de treino de musculação.
- Registro de séries.
- Troca de exercícios.
- Configuração dos equipamentos da academia.

## Comandos úteis

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend pytest
```


## v0.9.1 Hotfix

Corrige CI do backend e e-mail seed inválido para validação Pydantic.
