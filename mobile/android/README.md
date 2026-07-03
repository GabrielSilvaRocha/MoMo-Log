# Mo2 LOG Android

Este diretorio agora cobre dois caminhos Android:

1. APK WebView offline para abrir o build local do frontend dentro do celular.
2. Trilha nativa futura com Kotlin, Jetpack Compose, Health Connect e WorkManager.

## APK WebView offline

O objetivo e usar na academia mesmo com o PC desligado. O APK carrega arquivos estaticos do frontend e a tela Academia offline salva series no armazenamento local do app.

Passos no PC:

1. Gere o frontend.

```bash
cd frontend
npm run build
```

2. Copie o conteudo de frontend/dist para mobile/android/app/src/main/assets/mo2log.

3. Abra mobile/android no Android Studio.

4. Gere o APK em Build > Build Bundle(s) / APK(s) > Build APK(s).

Observacao: este repositorio nao inclui Gradle Wrapper ainda. Se o Android Studio criar ou baixar o Gradle, o projeto passa a gerar o APK localmente.

## Trilha nativa futura

- Kotlin
- Jetpack Compose
- Health Connect client
- WorkManager sync
- Retrofit/OkHttp API client

O contrato de sincronizacao Health Connect permanece em app/src/main/java/br/com/mo2log/mobile/sync/HealthConnectSyncContract.kt.

## v8.2.0

O app Android agora e nativo, pessoal e local. A MainActivity em Kotlin guarda treinos e series em SharedPreferences, sem WebView e sem backend.
