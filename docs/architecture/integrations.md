# Integrações

## Strava

O Strava é a fonte oficial das corridas no Mo² LOG.

Fluxo v1.1.0:

1. Frontend chama `GET /api/v1/auth/strava/authorize?user_id=1`.
2. Backend gera a URL de autorização do Strava.
3. Usuário autoriza no Strava.
4. Strava redireciona para `/api/v1/auth/strava/callback`.
5. Backend troca o `code` por `access_token` e `refresh_token`.
6. `POST /api/v1/strava/sync?user_id=1` importa atividades reais quando os tokens existem.
7. Sem tokens, o modo mock continua disponível para desenvolvimento local.


## Running Sources v1.2.0

O Mo² LOG passa a trabalhar com múltiplas fontes de corrida:

- `manual_treadmill`: lançamento manual de corridas na esteira.
- `manual_outdoor`: lançamento manual de corridas externas.
- `strava`: integração real opcional.
- `strava_mock`: dados simulados para desenvolvimento.
- `samsung_health` e `health_connect`: fontes planejadas para versões futuras.

Essa decisão reduz dependência de APIs pagas ou fechadas e atende ao cenário principal de uso: corridas majoritariamente na esteira.
