# Intelligence API

Base: `/api/v1/intelligence`

## Weekly insights

`GET /weekly-insights?user_id=1&reference_date=2026-06-29`

Retorna score híbrido, métricas da semana, insights e recomendações.

## Planned vs done

`GET /planned-vs-done?user_id=1&reference_date=2026-06-29`

Compara sessões planejadas, concluídas, adaptadas e puladas por tipo.

## Forecast 5k

`GET /forecast-5k?user_id=1`

Retorna uma previsão simples de 5 km baseada no pace das corridas registradas.
