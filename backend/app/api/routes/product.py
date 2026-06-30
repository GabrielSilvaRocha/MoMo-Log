from fastapi import APIRouter

from app.core.config import get_settings

router = APIRouter(prefix="/product", tags=["product"])


@router.get("/mvp-status")
def get_mvp_status() -> dict:
    settings = get_settings()
    modules = [
        {"key": "dashboard", "label": "Dashboard", "status": "stable", "description": "Resumo diário e semanal com próximos treinos, progresso, metas e insights."},
        {"key": "planning", "label": "Planejamento", "status": "stable", "description": "Criação, edição e remoção de sessões de musculação, corrida, mobilidade e descanso."},
        {"key": "workout", "label": "Execução de treino", "status": "stable", "description": "Registro de séries, checklist, volume do treino, RPE e cronômetro de descanso."},
        {"key": "adaptation", "label": "Adaptação", "status": "stable", "description": "Sugestões de substituição com penalidade para equipamento indisponível e bônus para favoritos."},
        {"key": "running", "label": "Corridas", "status": "stable", "description": "Cadastro manual de esteira, sincronização mock e Strava opcional."},
        {"key": "history", "label": "Histórico", "status": "stable", "description": "Consulta de sessões por período, status e tipo."},
        {"key": "reports", "label": "Relatórios", "status": "stable", "description": "Resumo por período e exportação CSV."},
        {"key": "analytics", "label": "Estatísticas", "status": "stable", "description": "Metas, recordes pessoais e insights iniciais."},
        {"key": "auth", "label": "Autenticação", "status": "stable", "description": "Cadastro, login, sessão local, perfil e preferências do usuário."},
    ]

    user_flows = [
        {"key": "execute_strength_workout", "label": "Executar treino de musculação", "coverage": 100},
        {"key": "swap_exercise", "label": "Trocar exercício por academia cheia", "coverage": 100},
        {"key": "mark_equipment_unavailable", "label": "Marcar equipamento inexistente", "coverage": 100},
        {"key": "manual_treadmill_run", "label": "Registrar corrida na esteira", "coverage": 100},
        {"key": "weekly_planning", "label": "Editar planejamento semanal", "coverage": 95},
        {"key": "reports_export", "label": "Exportar relatório CSV", "coverage": 90},
        {"key": "strava_real_oauth", "label": "Strava real via OAuth", "coverage": 60},
        {"key": "health_connect", "label": "Health Connect / Samsung Health", "coverage": 10},
        {"key": "user_authentication", "label": "Entrar, cadastrar e manter sessão local", "coverage": 100},
        {"key": "user_preferences", "label": "Configurar fonte padrão de corrida e metas semanais", "coverage": 100},
    ]

    next_priorities = [
        "Importação GPX/CSV/FIT",
        "Health Connect / Samsung Health no app mobile",
        "Importação GPX/CSV/FIT",
        "Deploy demonstrável para portfólio",
    ]

    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "milestone": "Usuários e Preferências",
        "status": "operational",
        "modules": modules,
        "user_flows": user_flows,
        "next_priorities": next_priorities,
    }


@router.get("/release-notes")
def get_release_notes() -> dict:
    return {
        "version": "3.0.0",
        "title": "Usuários, autenticação e preferências",
        "highlights": [
            "Cadastro, login e login demo com token assinado.",
            "Tela de Perfil com dados pessoais e preferências de treino.",
            "Fonte padrão de corrida configurável, com esteira manual como preferência inicial.",
            "Preferências persistentes para metas semanais e observações da academia.",
        ],
    }
