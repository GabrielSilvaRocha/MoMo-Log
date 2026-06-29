# Changelog

## v1.2.0 - Running Sources

- Alterada a estratégia de corrida para múltiplas fontes.
- Adicionado cadastro manual de corrida na esteira pelo frontend.
- Strava passa a ser fonte opcional, não bloqueante.
- Adicionada comunicação visual para Samsung Health / Health Connect como roadmap.
- Adicionado suporte de criação manual usando `source=manual_treadmill`.

## v1.1.0

### Added
- Strava OAuth foundation.
- Endpoint para gerar URL de autorização do Strava.
- Callback OAuth com troca de authorization code por access token e refresh token.
- Sincronização real de atividades quando tokens estão configurados.
- Fallback mock preservado para desenvolvimento local sem credenciais.
- Botão “Conectar Strava” no frontend.

## v1.0.0

### Added
- MVP UI shell com Dashboard, Treino do dia, Corridas, Estatísticas, Metas e Exercícios.
