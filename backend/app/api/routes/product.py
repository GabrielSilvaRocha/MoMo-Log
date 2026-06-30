from fastapi import APIRouter

from app.core.config import get_settings

router = APIRouter(prefix="/product", tags=["product"])


@router.get("/mvp-status")
def get_mvp_status() -> dict:
    settings = get_settings()
    modules = [
        {"key": "dashboard", "label": "Dashboard", "status": "stable", "description": "Resumo diário e semanal com próximos treinos, progresso, metas e insights."},
        {"key": "planning", "label": "Planejamento", "status": "stable", "description": "Criação, edição e remoção de sessões de musculação, corrida, mobilidade e descanso."},
        {"key": "templates", "label": "Templates", "status": "stable", "description": "Biblioteca de treinos reutilizáveis e criação rápida de sessões planejadas."},
        {"key": "workout", "label": "Execução de treino", "status": "stable", "description": "Registro de séries, checklist, volume do treino, RPE e cronômetro de descanso."},
        {"key": "adaptation", "label": "Adaptação", "status": "stable", "description": "Sugestões de substituição com penalidade para equipamento indisponível e bônus para favoritos."},
        {"key": "running", "label": "Corridas", "status": "stable", "description": "Running Coach com objetivo de 5 km, plano de esteira, execução por distância, ajuste de velocidade e progressão automática."},
        {"key": "history", "label": "Histórico", "status": "stable", "description": "Consulta de sessões por período, status e tipo."},
        {"key": "reports", "label": "Relatórios", "status": "stable", "description": "Resumo por período e exportação CSV."},
        {"key": "analytics", "label": "Estatísticas", "status": "stable", "description": "Metas, recordes pessoais e insights iniciais."},
        {"key": "intelligence", "label": "Inteligência", "status": "beta", "description": "Insights semanais, comparação planejado vs realizado e previsão simples de 5 km."},
        {"key": "auth", "label": "Autenticação", "status": "stable", "description": "Cadastro, login, sessão local, perfil e preferências do usuário."},
    ]

    user_flows = [
        {"key": "execute_strength_workout", "label": "Executar treino de musculação", "coverage": 100},
        {"key": "swap_exercise", "label": "Trocar exercício por academia cheia", "coverage": 100},
        {"key": "mark_equipment_unavailable", "label": "Marcar equipamento inexistente", "coverage": 100},
        {"key": "manual_treadmill_run", "label": "Registrar corrida na esteira", "coverage": 100},
        {"key": "weekly_planning", "label": "Editar planejamento semanal", "coverage": 95},
        {"key": "schedule_template", "label": "Criar sessão a partir de template", "coverage": 100},
        {"key": "reports_export", "label": "Exportar relatório CSV", "coverage": 90},
        {"key": "running_coach", "label": "Running Coach de esteira", "coverage": 75},
        {"key": "health_connect", "label": "Health Connect / Samsung Health", "coverage": 10},
        {"key": "user_authentication", "label": "Entrar, cadastrar e manter sessão local", "coverage": 100},
        {"key": "user_preferences", "label": "Configurar fonte padrão de corrida e metas semanais", "coverage": 100},
    ]

    next_priorities = [
        "Templates customizáveis criados pelo usuário",
        "Personalização da progressão do plano de corrida",
        "Evolução de carga por exercício",
        "Previsões por distância e prova-alvo",
        "Health Connect / Samsung Health no app mobile",
    ]

    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "milestone": "Running Coach Auto Progression",
        "status": "operational",
        "modules": modules,
        "user_flows": user_flows,
        "next_priorities": next_priorities,
    }


@router.get("/release-notes")
def get_release_notes() -> dict:
    return {
        "version": "6.2.0",
        "title": "Running Coach Auto Progression",
        "highlights": [
            "Etapas avançam automaticamente quando distância ou tempo chega a zero.",
            "Backend conclui logs de etapa e encerra a sessão no último bloco.",
            "Painel da esteira ganhou ação manual para concluir etapa quando necessário.",
            "Execução guiada ficou contínua entre aquecimento, blocos principais e desaquecimento.",
        ],
    }
