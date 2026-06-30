# Changelog

## 4.0.1 - Security Hotfix

- Remove senha demo hardcoded do README, documentação, frontend e backend.
- Demo login permanece disponível sem senha versionada.
- Adiciona migration para limpar hash antigo do usuário demo local.
- Atualiza placeholders de ambiente para reduzir alertas de secret scanning.

## v4.0.0 - Intelligence Core

- Adicionada tela **Inteligência** no frontend.
- Adicionados endpoints `/intelligence/weekly-insights`, `/intelligence/planned-vs-done` e `/intelligence/forecast-5k`.
- Adicionado score híbrido semanal.
- Adicionados insights e recomendações acionáveis.
- Adicionada previsão simples para 5 km.
- Atualizada documentação da API e produto.


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
