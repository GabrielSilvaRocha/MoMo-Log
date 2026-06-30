# Relatórios - API v1

A área de relatórios consolida dados de musculação, corrida, sessões planejadas e execução real por período.

## Overview

```http
GET /api/v1/reports/overview?user_id=1&date_from=2026-06-01&date_to=2026-06-30
```

Retorna métricas agregadas como:

- sessões totais e concluídas;
- consistência;
- volume de musculação;
- séries registradas;
- distância de corrida;
- quantidade de corridas na esteira;
- pace médio;
- insights do período.

## Exportações CSV

```http
GET /api/v1/reports/export/sessions.csv?user_id=1
GET /api/v1/reports/export/running.csv?user_id=1
GET /api/v1/reports/export/strength.csv?user_id=1
```

Essas rotas geram arquivos compatíveis com Excel, Power BI e análises externas.
