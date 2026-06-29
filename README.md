# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

O objetivo do projeto é centralizar planejamento, execução, adaptação e evolução dos treinos, com foco em:

- treinos de musculação;
- troca inteligente de exercícios;
- configuração dos equipamentos disponíveis na academia;
- integração futura com Strava como fonte oficial das corridas;
- dashboard de evolução semanal.

## Stack inicial

- Python 3.13
- FastAPI
- SQLAlchemy 2
- Alembic
- PostgreSQL 16
- Docker Compose

## Como executar

```bash
docker compose up --build
```

A API ficará disponível em:

```text
http://localhost:8000
```

Health check:

```text
http://localhost:8000/api/v1/health
```

## Rodar migrations

Com os containers ativos:

```bash
docker compose exec backend alembic upgrade head
```

## Testar endpoint de usuários

```text
http://localhost:8000/api/v1/users
```

Retorno esperado após migration:

```json
[]
```

## Documentação interativa

```text
http://localhost:8000/docs
```
