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
        {"key": "strava", "label": "Strava", "status": "optional", "detail": "Integração opcional; cadastro manual de esteira permanece como fonte principal"},
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
        "version": "5.0.0",
        "title": "Checklist de deploy e portfólio",
        "items": [
            {"key": "env", "label": "Variáveis de ambiente separadas por ambiente", "status": "ready"},
            {"key": "secrets", "label": "Sem credenciais reais versionadas", "status": "ready"},
            {"key": "backend_ci", "label": "CI backend com PostgreSQL e migrations", "status": "ready"},
            {"key": "frontend_ci", "label": "CI frontend com build TypeScript/Vite", "status": "ready"},
            {"key": "docker", "label": "Docker Compose local e produção base", "status": "ready"},
            {"key": "docs", "label": "Documentação de produto, deploy e segurança", "status": "ready"},
            {"key": "cloud", "label": "Deploy em cloud pública", "status": "planned"},
        ],
        "recommended_next_targets": [
            "Publicar backend em Railway, Render ou Fly.io",
            "Publicar frontend em Vercel ou Netlify",
            "Usar PostgreSQL gerenciado em ambiente de demonstração",
            "Configurar domínio e variáveis de produção",
        ],
    }
