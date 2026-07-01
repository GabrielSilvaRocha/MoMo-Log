from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.database.dependencies import get_db

router = APIRouter(prefix="/ops", tags=["operations"])


@router.get("/status")
def get_operations_status(db: Session = Depends(get_db)) -> dict:
    settings = get_settings()
    database_ok = True
    database_message = "ok"

    try:
        db.execute(text("SELECT 1"))
    except Exception as exc:  # pragma: no cover - defensive readiness branch
        database_ok = False
        database_message = str(exc)

    services = [
        {"key": "backend", "label": "Backend API", "status": "ok", "detail": "FastAPI online"},
        {"key": "database", "label": "PostgreSQL", "status": "ok" if database_ok else "error", "detail": database_message},
        {"key": "frontend", "label": "Frontend", "status": "external", "detail": "Validado pelo build do Vite e Docker"},
        {"key": "running_coach", "label": "Running Coach", "status": "ok", "detail": "Plano por objetivo, progressão personalizada e execução guiada de esteira"},
        {"key": "mobile_sync", "label": "Mobile Sync", "status": "designed", "detail": "Roteiro Health Connect / Samsung Health preparado para app mobile"},
        {"key": "portfolio", "label": "Portfólio", "status": "ready", "detail": "Checklist, demo script e screenshots-alvo disponíveis"},
    ]

    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "environment": settings.app_env,
        "status": "operational" if database_ok else "degraded",
        "services": services,
    }


@router.get("/deployment-checklist")
def get_deployment_checklist() -> dict:
    return {
        "version": "7.0.0",
        "title": "Checklist de deploy e portfólio",
        "items": [
            {"key": "env", "label": "Variáveis de ambiente separadas por ambiente", "status": "ready", "detail": "Exemplos locais e produção separados."},
            {"key": "secrets", "label": "Sem credenciais reais versionadas", "status": "ready", "detail": "Arquivos de exemplo usam placeholders."},
            {"key": "backend_ci", "label": "CI backend com PostgreSQL e migrations", "status": "ready", "detail": "Testes backend cobrem fluxos centrais."},
            {"key": "frontend_ci", "label": "CI frontend com build TypeScript/Vite", "status": "ready", "detail": "Build de produção validado no container."},
            {"key": "docker", "label": "Docker Compose local e produção base", "status": "ready", "detail": "Ambientes local e prod documentados."},
            {"key": "docs", "label": "Documentação de produto, deploy e segurança", "status": "ready", "detail": "README, docs e status operacional expõem o roteiro."},
            {"key": "mobile_sync", "label": "Roteiro Health Connect / Samsung Health", "status": "designed", "detail": "Contrato de readiness e mapeamento de dados criado."},
            {"key": "portfolio", "label": "Roteiro de demo e screenshots", "status": "ready", "detail": "Pacote v7.0.0 preparado para apresentação."},
            {"key": "cloud", "label": "Deploy em cloud pública", "status": "planned", "detail": "Próximo passo fora do repositório local."},
        ],
        "recommended_next_targets": [
            "Publicar backend em Railway, Render ou Fly.io",
            "Publicar frontend em Vercel ou Netlify",
            "Usar PostgreSQL gerenciado em ambiente de demonstração",
            "Configurar domínio e variáveis de produção",
        ],
        "demo_script": [
            "Abrir Dashboard e mostrar score híbrido, foco da semana e mix força/corrida.",
            "Gerar plano no Running Coach com preferências de progressão.",
            "Executar um treino guiado por distância e ajustar velocidade da esteira.",
            "Mostrar Templates, adaptação por equipamento e evolução de carga.",
            "Fechar no painel Deploy com checklist, roteiro mobile e próximos alvos cloud.",
        ],
        "screenshot_targets": [
            {"key": "dashboard", "label": "Dashboard híbrido", "route": "/"},
            {"key": "running", "label": "Running Coach", "route": "/running"},
            {"key": "templates", "label": "Templates personalizados", "route": "/templates"},
            {"key": "intelligence", "label": "Inteligência e forecasts", "route": "/intelligence"},
            {"key": "deploy", "label": "Deploy e portfólio", "route": "/deploy"},
        ],
        "portfolio_summary": "Mo² LOG é um app full-stack de treino híbrido com FastAPI, PostgreSQL, React, Docker, execução guiada, inteligência de treino e preparação mobile.",
    }
