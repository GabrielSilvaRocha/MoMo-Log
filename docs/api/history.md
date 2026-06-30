# History API

## GET `/api/v1/history/sessions`

Lista sessões do usuário com filtros.

### Query params

- `user_id` obrigatório
- `date_from` opcional
- `date_to` opcional
- `session_type` opcional
- `status` opcional
- `limit` opcional, máximo 200

## GET `/api/v1/history/summary`

Retorna agregados do período.

### Métricas

- Total de sessões
- Sessões concluídas
- Sessões adaptadas
- Sessões puladas
- Sessões de musculação
- Sessões de corrida
- Volume total de musculação
- Total de séries
- Distância corrida
- Tempo de corrida
- Pace médio
- Taxa de conclusão
