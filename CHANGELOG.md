# Changelog

## v6.0.0 - Templates e Workout Builder

### Adicionado
- Tela **Templates** no frontend.
- Endpoint `GET /api/v1/workout-templates`.
- Endpoint `GET /api/v1/workout-templates/{template_id}`.
- Endpoint `POST /api/v1/workout-templates/{template_id}/schedule`.
- Models `WorkoutTemplate` e `WorkoutTemplateExercise`.
- Migration `20260630_0008_create_workout_templates`.
- Templates iniciais para:
  - Pernas — Força e base
  - Superior — Empurrar
  - Costas + Core
- Criação automática de sessão de musculação a partir de template.
- Cópia automática dos exercícios planejados para a sessão criada.
- Testes básicos para os endpoints de template.

### Atualizado
- Versão da aplicação para `6.0.0`.
- Tela MVP com módulo Templates.
- Release notes do produto.
- Navegação principal do frontend.

### Objetivo
Reduzir o atrito no planejamento semanal. Em vez de criar todos os exercícios manualmente, o usuário pode reutilizar templates e gerar sessões completas em poucos segundos.
