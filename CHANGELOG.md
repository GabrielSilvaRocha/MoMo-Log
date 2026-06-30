# Changelog

## v3.0.0 — Usuários, autenticação e preferências

### Adicionado

- Cadastro de usuário.
- Login com token assinado no backend.
- Login demo para usar o banco populado.
- Endpoint `POST /api/v1/auth/register`.
- Endpoint `POST /api/v1/auth/login`.
- Endpoint `POST /api/v1/auth/demo-login`.
- Endpoint `GET /api/v1/auth/me`.
- Endpoint `GET /api/v1/profile/{user_id}`.
- Endpoint `PATCH /api/v1/profile/{user_id}`.
- Endpoint `GET /api/v1/profile/{user_id}/preferences`.
- Endpoint `PATCH /api/v1/profile/{user_id}/preferences`.
- Tabela `user_preferences`.
- Tela de autenticação no frontend.
- Tela de perfil e preferências.
- Sessão local no frontend usando token.
- Uso do usuário autenticado nos fluxos principais.

### Alterado

- Versão da aplicação para `3.0.0`.
- Produto agora passa do MVP consolidado para camada de usuário real.
- Strava permanece opcional; a fonte padrão de corrida fica configurável por usuário.

### Corrigido

- Demo seed agora possui senha configurada para autenticação local.
