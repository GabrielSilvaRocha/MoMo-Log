from fastapi import APIRouter

from app.api.routes.exercises import router as exercises_router
from app.api.routes.dashboard import router as dashboard_router
from app.api.routes.health import router as health_router
from app.api.routes.analytics import router as analytics_router
from app.api.routes.adaptation import router as adaptation_router
from app.api.routes.goals import router as goals_router
from app.api.routes.history import router as history_router
from app.api.routes.reports import router as reports_router
from app.api.routes.running import router as running_router
from app.api.routes.strava_auth import router as strava_auth_router
from app.api.routes.training import router as training_router
from app.api.routes.user_gym_equipment import router as user_gym_equipment_router
from app.api.routes.users import router as users_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(health_router)
api_router.include_router(users_router)
api_router.include_router(exercises_router)
api_router.include_router(user_gym_equipment_router)
api_router.include_router(training_router)
api_router.include_router(running_router)
api_router.include_router(strava_auth_router)
api_router.include_router(dashboard_router)
api_router.include_router(goals_router)
api_router.include_router(analytics_router)
api_router.include_router(adaptation_router)
api_router.include_router(history_router)
api_router.include_router(reports_router)
