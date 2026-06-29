# Changelog

## v0.3.0 - Foundation corrigida

### Adicionado

- Estrutura backend com FastAPI.
- Configuração centralizada com Pydantic Settings.
- SQLAlchemy 2 configurado.
- Alembic configurado.
- Primeiro model `User`.
- Migration inicial da tabela `users`.
- Endpoint `GET /api/v1/users`.
- Endpoint `GET /api/v1/health`.
- Estrutura de documentação.
- GitHub Actions básico.

### Corrigido

- Removida importação circular entre `Base`, `User` e `app.models`.
