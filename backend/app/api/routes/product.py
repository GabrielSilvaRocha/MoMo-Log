from fastapi import APIRouter

from app.core.config import get_settings

router = APIRouter(prefix="/product", tags=["product"])


@router.get("/mvp-status")
def get_mvp_status() -> dict:
    settings = get_settings()
    modules = [
        {"key": "dashboard", "label": "Dashboard", "status": "stable", "description": "Resumo diário e semanal com próximos treinos, progresso, metas e insights."},
        {"key": "planning", "label": "Planejamento", "status": "stable", "description": "Criação, edição e remoção de sessões de musculação, corrida, mobilidade e descanso."},
        {"key": "templates", "label": "Templates", "status": "stable", "description": "Templates reutilizáveis com criação personalizada, arquivamento e agendamento rápido de sessões planejadas."},
        {"key": "workout", "label": "Execução de treino", "status": "stable", "description": "Registro de séries, checklist, volume do treino, RPE, descanso e sugestão de evolução de carga."},
        {"key": "adaptation", "label": "Adaptação", "status": "stable", "description": "Sugestões de substituição com penalidade para equipamento indisponível e bônus para favoritos."},
        {"key": "running", "label": "Corridas", "status": "stable", "description": "Running Coach com objetivo de 5 km, preferências de progressão, plano de esteira, execução por distância, ajuste de velocidade e progressão automática."},
        {"key": "history", "label": "Histórico", "status": "stable", "description": "Consulta de sessões por período, status e tipo."},
        {"key": "reports", "label": "Relatórios", "status": "stable", "description": "Resumo por período e exportação CSV."},
        {"key": "analytics", "label": "Estatísticas", "status": "stable", "description": "Metas, recordes pessoais e insights iniciais."},
        {"key": "intelligence", "label": "Inteligência", "status": "beta", "description": "Insights semanais, planejado vs realizado e previsões por distância/prova-alvo."},
        {"key": "auth", "label": "Autenticação", "status": "stable", "description": "Cadastro, login, sessão local, perfil e preferências do usuário."},
    ]

    user_flows = [
        {"key": "execute_strength_workout", "label": "Executar treino de musculação", "coverage": 100},
        {"key": "strength_load_progression", "label": "Receber sugestão de evolução de carga", "coverage": 80},
        {"key": "swap_exercise", "label": "Trocar exercício por academia cheia", "coverage": 100},
        {"key": "mark_equipment_unavailable", "label": "Marcar equipamento inexistente", "coverage": 100},
        {"key": "manual_treadmill_run", "label": "Registrar corrida na esteira", "coverage": 100},
        {"key": "weekly_planning", "label": "Editar planejamento semanal", "coverage": 95},
        {"key": "schedule_template", "label": "Criar sessão a partir de template", "coverage": 100},
        {"key": "custom_template", "label": "Criar template personalizado", "coverage": 100},
        {"key": "reports_export", "label": "Exportar relatório CSV", "coverage": 90},
        {"key": "running_coach", "label": "Running Coach de esteira", "coverage": 90},
        {"key": "race_forecast", "label": "Prever tempo por distância-alvo", "coverage": 80},
        {"key": "health_connect", "label": "Health Connect / Samsung Health", "coverage": 60},
        {"key": "user_authentication", "label": "Entrar, cadastrar e manter sessão local", "coverage": 100},
        {"key": "user_preferences", "label": "Configurar fonte padrão de corrida e metas semanais", "coverage": 100},
        {"key": "portfolio_demo", "label": "Apresentar projeto como portfólio", "coverage": 100},
    ]

    next_priorities = [
        "Adicionar editor local para alterar o plano pessoal no celular",
        "Adicionar resumo final da sessao com volume, series e exercicios concluidos",
    ]

    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "milestone": "Guided 5K Running Coach",
        "status": "operational",
        "release_focus": "Reformular a aba Corrida no Android com semana atual, planejamento completo e treino guiado por fases.",
        "modules": modules,
        "user_flows": user_flows,
        "next_priorities": next_priorities,
    }


@router.get("/release-notes")
def get_release_notes() -> dict:
    return {
        "version": "9.3.0",
        "title": "Guided 5K Running Coach",
        "highlights": [
            "Aba Corrida ganhou bloco Essa Semana com os 5 treinos e checkbox de conclusao.",
            "Treinos podem ser marcados como concluidos manualmente quando feitos sem o app.",
            "Planejamento completo mostra 6 semanas de progressao ate a meta de 5 km.",
            "Treino guiado por fases tem timer inicial de 5 segundos, contagem regressiva, km restantes e ajuste de velocidade com botoes -/+.",
            "Coach por voz avisa quando faltam 30 segundos e informa a proxima etapa.",
            "Build Android atualizado para versionCode 930.",
        ],
    }
