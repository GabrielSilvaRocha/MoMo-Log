# Changelog

## v5.0.0 - Deploy, portfólio e qualidade operacional

### Adicionado

- Tela **Deploy** no frontend.
- Endpoint `GET /api/v1/ops/status`.
- Endpoint `GET /api/v1/ops/deployment-checklist`.
- Docker Compose de produção base.
- Dockerfile de produção do backend.
- Dockerfile de produção do frontend com Nginx.
- Documentação de deploy, portfólio e política de secrets.
- Testes para os endpoints operacionais.

### Alterado

- Versão da aplicação para `5.0.0`.
- README consolidado para uso local, deploy e portfólio.
- Notas de release e status do produto atualizados.

### Segurança

- Mantida a decisão de não versionar credenciais reais.
- Demo local permanece sem senha versionada.

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
