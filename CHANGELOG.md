# Changelog

## v2.0.0 — MVP Consolidado

### Adicionado
- Endpoint `/api/v1/product/mvp-status` com visão consolidada dos módulos do produto.
- Endpoint `/api/v1/product/release-notes` com destaques da versão.
- Tela `MVP` no frontend com status dos módulos, checklist de fluxos e prioridades.
- Componentes reutilizáveis `LoadingState`, `EmptyState` e `SectionHeader`.
- Workflows `Backend CI` e `Frontend CI`.
- `Makefile` com comandos úteis para desenvolvimento local.

### Consolidado
- MVP agora agrupa musculação, corrida manual/esteira, planejamento, adaptação, histórico, relatórios, metas e estatísticas.
- Strava permanece como integração opcional, sem bloquear o uso principal do app.

### Corrigido
- Limpeza de artefatos gerados (`__pycache__`, `tsbuildinfo`) no pacote da release.
- Documentação e versão atualizadas para `2.0.0`.

## v1.7.0 — Relatórios e Exportação
- Tela de relatórios.
- Exportação CSV de sessões, corridas e musculação.

## v1.6.0 — Histórico de Treinos
- Histórico de sessões e resumo por período.
