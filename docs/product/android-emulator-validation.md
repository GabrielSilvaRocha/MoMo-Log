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
