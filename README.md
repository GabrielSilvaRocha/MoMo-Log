# Mo² LOG

Mo² LOG é um app de treino híbrido para musculação e corrida. Ele centraliza planejamento, execução, adaptação e evolução do treino.

## v1.3.0

Esta versão adiciona o módulo de Planejamento semanal editável.

### Principais módulos

- Dashboard semanal
- Treino do dia
- Planejamento semanal
- Corridas com múltiplas fontes
- Cadastro manual de corrida na esteira
- Biblioteca de exercícios
- Troca de exercícios por disponibilidade da academia
- Metas, recordes e estatísticas

## Executar localmente

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
```

Backend:

```text
http://localhost:8000/api/v1/health
```

Frontend:

```text
http://localhost:5173
```
