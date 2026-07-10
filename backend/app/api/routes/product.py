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
        {"key": "strength_load_progression", "label": "Receber sugestão de evolução de carga", "coverage": 90},
        {"key": "swap_exercise", "label": "Trocar exercício por academia cheia", "coverage": 100},
        {"key": "mark_equipment_unavailable", "label": "Marcar equipamento inexistente", "coverage": 100},
        {"key": "manual_treadmill_run", "label": "Registrar corrida na esteira", "coverage": 100},
        {"key": "weekly_planning", "label": "Editar planejamento semanal", "coverage": 100},
        {"key": "schedule_template", "label": "Criar sessão a partir de template", "coverage": 100},
        {"key": "custom_template", "label": "Criar template personalizado", "coverage": 100},
        {"key": "reports_export", "label": "Exportar relatório CSV", "coverage": 90},
        {"key": "running_coach", "label": "Running Coach de esteira", "coverage": 95},
        {"key": "race_forecast", "label": "Prever tempo por distância-alvo", "coverage": 80},
        {"key": "health_connect", "label": "Health Connect / Samsung Health", "coverage": 60},
        {"key": "user_authentication", "label": "Entrar, cadastrar e manter sessão local", "coverage": 100},
        {"key": "user_preferences", "label": "Configurar fonte padrão de corrida e metas semanais", "coverage": 100},
        {"key": "portfolio_demo", "label": "Apresentar projeto como portfólio", "coverage": 100},
    ]

    next_priorities = [
        "Acompanhar prontidao diaria e ajustar detalhes finos",
        "Preparar refinamentos pos-v10 com base no treino real",
    ]

    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "milestone": "Daily Readiness Check-in",
        "status": "operational",
        "release_focus": "Adicionar check-in diario de prontidao para ajustar intensidade do treino pessoal.",
        "modules": modules,
        "user_flows": user_flows,
        "next_priorities": next_priorities,
    }


@router.get("/release-notes")
def get_release_notes() -> dict:
    return {
        "version": "10.1.0",
        "title": "Daily Readiness Check-in",
        "highlights": [
            "Home ganhou Check-in Rapido com estados Verde, Amarelo e Vermelho.",
            "Check-in do dia alimenta a linha de prontidao do Cockpit V10.",
            "Checklist de continuidade agora acompanha se o check-in diario foi feito.",
            "Coach e Insights passam a considerar a prontidao registrada no dia.",
            "Estado Verde orienta seguir o plano com progressao controlada.",
            "Estado Amarelo recomenda manter moderado e evitar volume extra.",
            "Estado Vermelho recomenda recuperacao ou treino reduzido.",
            "Build Android atualizado para versionCode 1010.",
        ],
    }
