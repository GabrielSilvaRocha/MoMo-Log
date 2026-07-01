# Android Health Connect Readiness

Mo² LOG v7.2 prepares the native Android path for importing running sessions from Health Connect.

## Android App Baseline

- Package: br.com.mo2log.mobile
- Language: Kotlin
- UI: Jetpack Compose
- Background sync: WorkManager
- Minimum SDK: 28
- Target SDK: 35

## Health Connect Permissions

- android.permission.health.READ_EXERCISE
- android.permission.health.READ_DISTANCE
- android.permission.health.READ_ACTIVE_CALORIES_BURNED
- android.permission.health.READ_HEART_RATE

Request permissions only after the user explicitly starts the connection flow.

## Sync Flow

1. Check Health Connect availability.
2. Request permissions.
3. Read ExerciseSessionRecord and DistanceRecord.
4. Convert records into RunningActivity payloads.
5. POST new activities to /api/v1/running-activities.
6. Store last_successful_sync_at locally.
7. Send conflicts to manual review when a treadmill activity already exists in the dedupe window.

## Dedupe Rule

Use a 15 minute window around start_date, compare elapsed_time_s and distance_m, and prefer the Health Connect record when the local activity is not manually confirmed.
