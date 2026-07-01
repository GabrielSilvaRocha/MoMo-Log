package br.com.mo2log.mobile.sync

data class HealthConnectSyncConfig(
    val packageName: String = "br.com.mo2log.mobile",
    val dedupeWindowMinutes: Long = 15,
    val initialBackfillDays: Long = 30,
    val dailyRefreshDays: Long = 3,
)

data class RunningActivityPayload(
    val userId: Long,
    val name: String,
    val distanceM: Int,
    val movingTimeS: Int,
    val elapsedTimeS: Int,
    val activityType: String = "run",
    val source: String = "health_connect",
    val startDate: String,
    val totalElevationGain: Double? = null,
)

object HealthConnectPermissions {
    val required = listOf(
        "android.permission.health.READ_EXERCISE",
        "android.permission.health.READ_DISTANCE",
        "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        "android.permission.health.READ_HEART_RATE",
    )
}
