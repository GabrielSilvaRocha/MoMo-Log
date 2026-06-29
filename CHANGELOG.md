# Changelog

## v1.0.0 - MVP UI Shell

### Added
- Tela de corridas com status Strava, botão de sincronização mock e lista de atividades.
- Tela de estatísticas com volume semanal, quilometragem, consistência, metas, recordes e insights.
- Tela de metas com atualização de progresso pelo frontend.
- Navegação principal expandida: Dashboard, Treino do dia, Corridas, Estatísticas, Metas e Exercícios.
- Utilitários de formatação para pace, duração e data/hora.

### Changed
- Versão da aplicação atualizada para `1.0.0`.
- Frontend passa a representar o primeiro MVP navegável do Mo² LOG.

### Notes
- A integração Strava ainda é mock/local. O OAuth real será implementado em release futura.

## v0.9.1 - Backend CI Hotfix

### Fixed
- E-mail seed ajustado para domínio válido.
- Backend CI estabilizado com PostgreSQL, migrations e variáveis de ambiente.

## v0.9.0 - Workout Execution UI

### Added
- Tela de execução de treino.
- Registro de séries.
- Troca de exercício no frontend.
- Configuração de equipamentos da academia.

## v0.8.0 - Frontend Foundation

### Added
- React + Vite + TypeScript + TailwindCSS.
- Dashboard inicial conectado à API.
