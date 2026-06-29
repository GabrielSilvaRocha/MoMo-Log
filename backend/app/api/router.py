from fastapi import APIRouter

from app.api.routes.exercises import router as exercises_router
from app.api.routes.health import router as health_router
from app.api.routes.user_gym_equipment import router as user_gym_equipment_router
from app.api.routes.users import router as users_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(health_router)
api_router.include_router(users_router)
api_router.include_router(exercises_router)
api_router.include_router(user_gym_equipment_router)
