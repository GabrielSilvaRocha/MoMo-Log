# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

## v6.1.0 — Running Coach Core

Esta versão reformula o módulo de corridas:

- Sem dependência de Strava ou integração externa.
- Objetivo inicial: prova de 5 km.
- Plano de corrida gerado até a data da prova.
- Execução guiada para esteira.
- Treinos contínuos e intervalados com etapas.
- Pace alvo, velocidade alvo, cronômetro regressivo e ajuste de velocidade com `+` e `-`.
- Registro manual de corridas permanece disponível para histórico.

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

## Login local

Use o botão **Entrar como Demo Local**. Não há senha demo versionada no repositório.
