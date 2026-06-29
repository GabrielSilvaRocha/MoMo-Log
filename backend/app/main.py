from fastapi import FastAPI

from app.api.routes.health import router as health_router
from app.api.routes.root import router as root_router
from app.core.config import get_settings
from app.core.logging import configure_logging

configure_logging()
settings = get_settings()

app = FastAPI(
    title=f"{settings.app_name} API",
    version=settings.app_version,
)

app.include_router(root_router, prefix="/api/v1")
app.include_router(health_router, prefix="/api/v1")
