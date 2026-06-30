# Changelog

## 6.1.0 — Running Coach Core

### Added
- Objetivo de corrida próprio para prova de 5 km.
- Plano de treinos de corrida gerado até a prova.
- Sessões de corrida com etapas de aquecimento, tiros, recuperação e desaquecimento.
- Execução guiada para esteira com pace alvo, velocidade alvo e cronômetro regressivo.
- Botões `+` e `-` para ajustar velocidade em 0,1 km/h durante a execução.
- Registro de ajustes de velocidade para análise futura.
- Endpoints `running-goals`, `running-plan`, `running-executions`, `running-step-logs`.
- Migration `20260630_0009_create_running_coach_core`.

### Changed
- O módulo Corridas deixa de depender de integrações externas.
- A interface de Corridas passa a priorizar planejamento e execução guiada.
- O dashboard passa a exibir Running Coach como status de corrida.

### Removed
- Rotas públicas de OAuth/sincronização Strava do roteador principal.
- Botões de conexão/sincronização externa na tela Corridas.
