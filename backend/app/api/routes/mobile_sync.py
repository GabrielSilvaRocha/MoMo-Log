from fastapi import APIRouter

router = APIRouter(prefix="/mobile-sync", tags=["mobile-sync"])


ANDROID_PERMISSIONS = [
    {
        "group": "exercise",
        "permissions": [
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        ],
        "reason": "Importar sessoes de corrida/caminhada e distancia total.",
    },
    {
        "group": "heart_rate",
        "permissions": ["android.permission.health.READ_HEART_RATE"],
        "reason": "Preparar analise futura de intensidade e recuperacao.",
    },
]

DATA_MAPPING = [
    {"external": "ExerciseSessionRecord", "mo2log": "RunningActivity", "status": "mapped"},
    {"external": "DistanceRecord", "mo2log": "distance_m", "status": "mapped"},
    {"external": "ExerciseRoute", "mo2log": "future_route_trace", "status": "planned"},
    {"external": "HeartRateRecord", "mo2log": "future_metric", "status": "planned"},
    {"external": "TotalCaloriesBurnedRecord", "mo2log": "future_metric", "status": "planned"},
]


@router.get("/readiness")
def get_mobile_sync_readiness() -> dict:
    return {
        "status": "android_ready",
        "target_platforms": ["Android Health Connect", "Samsung Health via Health Connect"],
        "android_app": {
            "package_name": "br.com.mo2log.mobile",
            "min_sdk": 28,
            "target_sdk": 35,
            "language": "Kotlin",
            "ui_stack": "Jetpack Compose",
            "sync_worker": "WorkManager",
        },
        "sync_strategy": {
            "direction": "import_first",
            "primary_activity_source": "Health Connect",
            "fallback_activity_source": "manual_treadmill",
            "conflict_policy": "prefer_external_activity_with_manual_review",
            "dedupe_window_minutes": 15,
        },
        "permission_groups": ANDROID_PERMISSIONS,
        "required_permissions": [permission for group in ANDROID_PERMISSIONS for permission in group["permissions"]],
        "data_mapping": DATA_MAPPING,
        "sync_windows": [
            {"key": "initial_backfill", "label": "Primeira sincronizacao", "lookback_days": 30},
            {"key": "daily_refresh", "label": "Atualizacao diaria", "lookback_days": 3},
            {"key": "manual_refresh", "label": "Atualizacao manual", "lookback_days": 7},
        ],
        "implementation_steps": [
            "Criar shell Android nativo em Kotlin com autenticação do usuário Mo² LOG.",
            "Solicitar permissões Health Connect apenas após ação explícita do usuário.",
            "Ler ExerciseSessionRecord e DistanceRecord por janela incremental.",
            "Normalizar sessões recentes como RunningActivity com source='health_connect'.",
            "Deduplicar por janela de início, duração e distância antes de persistir.",
            "Exibir revisão manual quando houver conflito com corrida registrada na esteira.",
        ],
    }


@router.get("/android-plan")
def get_android_sync_plan() -> dict:
    return {
        "status": "ready_for_scaffold",
        "modules": [
            {"path": "mobile/android/app", "purpose": "Aplicativo Android nativo"},
            {"path": "mobile/android/app/src/main/java/br/com/mo2log/mobile/sync", "purpose": "Health Connect client e worker"},
            {"path": "mobile/android/app/src/main/java/br/com/mo2log/mobile/network", "purpose": "Cliente HTTP para API Mo² LOG"},
        ],
        "kotlin_dependencies": [
            "androidx.health.connect:connect-client",
            "androidx.work:work-runtime-ktx",
            "androidx.lifecycle:lifecycle-runtime-ktx",
            "androidx.activity:activity-compose",
            "com.squareup.retrofit2:retrofit",
            "com.squareup.okhttp3:logging-interceptor",
        ],
        "api_payload": {
            "endpoint": "/api/v1/running-activities",
            "method": "POST",
            "source": "health_connect",
            "fields": [
                "user_id",
                "name",
                "distance_m",
                "moving_time_s",
                "elapsed_time_s",
                "activity_type",
                "source",
                "start_date",
                "total_elevation_gain",
            ],
        },
        "worker_flow": [
            "Verificar disponibilidade do Health Connect.",
            "Checar permissões concedidas.",
            "Buscar registros desde last_successful_sync_at.",
            "Converter registros para payload da API.",
            "Enviar registros novos e armazenar sync cursor local.",
            "Marcar conflitos para revisão manual.",
        ],
    }
