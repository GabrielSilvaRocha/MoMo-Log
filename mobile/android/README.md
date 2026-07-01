# Mo² LOG Android Scaffold

This folder documents the native Android direction for v7.2. It is intentionally lightweight: the production app should be created from this scaffold in Android Studio.

## Planned Stack

- Kotlin
- Jetpack Compose
- Health Connect client
- WorkManager sync
- Retrofit/OkHttp API client

## First Implementation Steps

1. Create an Android Studio project with package br.com.mo2log.mobile.
2. Add Health Connect and WorkManager dependencies.
3. Copy the sync contract from app/src/main/java/br/com/mo2log/mobile/sync/HealthConnectSyncContract.kt.
4. Implement token storage and API base URL configuration.
5. Connect imported activities to /api/v1/running-activities.
