# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

## v6.1.1 — Running Coach Execution Hotfix

Esta versão corrige e melhora a execução guiada do módulo Corridas:

- Timer de 5 segundos antes de iniciar a primeira etapa.
- Execução baseada em distância, com km regressivos quando a etapa possui distância planejada.
- Tempo restante recalculado automaticamente quando a velocidade da esteira muda.
- Botões `+` e `-` continuam ajustando a velocidade em 0,1 km/h.
- Pace exibido a partir da velocidade atual da esteira.
- Etapas de descanso continuam controladas por tempo.
- Teste operacional de versão estabilizado para não quebrar a cada release.

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
