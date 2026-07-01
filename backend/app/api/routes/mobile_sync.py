from fastapi import APIRouter

router = APIRouter(prefix="/mobile-sync", tags=["mobile-sync"])


@router.get("/readiness")
def get_mobile_sync_readiness() -> dict:
    return {
        "status": "designed",
        "target_platforms": ["Android Health Connect", "Samsung Health"],
        "sync_strategy": {
            "direction": "import_first",
            "primary_activity_source": "Health Connect",
            "fallback_activity_source": "manual_treadmill",
            "conflict_policy": "prefer_external_activity_with_manual_review",
        },
        "required_permissions": [
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
            "android.permission.health.READ_HEART_RATE",
        ],
        "data_mapping": [
            {"external": "ExerciseSessionRecord", "mo2log": "RunningActivity", "status": "mapped"},
            {"external": "DistanceRecord", "mo2log": "distance_m", "status": "mapped"},
            {"external": "HeartRateRecord", "mo2log": "future_metric", "status": "planned"},
            {"external": "TotalCaloriesBurnedRecord", "mo2log": "future_metric", "status": "planned"},
        ],
        "implementation_steps": [
            "Criar shell mobile com autenticação local do usuário Mo² LOG.",
            "Solicitar permissões Health Connect apenas após ação explícita do usuário.",
            "Importar sessões recentes como RunningActivity com source='health_connect'.",
            "Deduplicar por janela de início, duração e distância antes de persistir.",
            "Exibir revisão manual quando houver conflito com corrida registrada na esteira.",
        ],
    }
