# Mo2 LOG v8.1.1 - Validacao no emulador Android

Objetivo: testar o APK no Android Studio antes de instalar no celular fisico.

## Criar emulador no Android Studio

1. Abra o Android Studio.
2. Va em Tools > Device Manager.
3. Clique em Create virtual device.
4. Escolha um Pixel recente.
5. Escolha uma imagem API 35 ou API 36. Se pedir download, aceite.
6. Finalize e clique no botao Play do emulador.

## Gerar APK corrigido

No terminal:

```powershell
cd C:\Users\CDP\OneDrive\Documentos\Projeto\MoMo-Log\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

APK:

```text
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Instalar no emulador

Com o emulador aberto:

```powershell
C:\Users\CDP\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r "C:\Users\CDP\OneDrive\Documentos\Projeto\MoMo-Log\mobile\android\app\build\outputs\apk\debug\app-debug.apk"
```

## Diagnostico de tela branca

Logs do WebView:

```powershell
C:\Users\CDP\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -s Mo2LogWebView chromium AndroidRuntime
```

O APK corrigido carrega:

```text
https://appassets.androidplatform.net/assets/mo2log/index.html?view=offline-workout
```

Esse caminho evita bloqueios comuns de JavaScript/CSS quando o WebView abre arquivos via file://.


## Validacao v8.1.3

Ao abrir o APK, ele deve ir direto para Academia offline sem pedir login.

Se aparecer failed to fetch, voce provavelmente tocou em Demo Local ou esta com build antigo instalado. Reinstale o APK gerado apos a v8.1.3.

### v8.1.3 - Android APK sem cache legado

- APK Android abre diretamente a Academia offline sem depender do backend.
- Service worker fica desativado no WebView asset host para evitar bundle antigo.
- Build Android atualizado para versionCode 813 e versionName 8.1.3.

## v8.1.4 - Android Offline-Only APK

- O APK Android deve mostrar apenas a entrada Academia offline no menu.
- Abas online como Corridas, Deploy e Treino do dia ficam escondidas no WebView Android.
- Se o usuario abrir o app fora de casa, o fluxo nao deve chamar backend nem exibir failed to fetch.
