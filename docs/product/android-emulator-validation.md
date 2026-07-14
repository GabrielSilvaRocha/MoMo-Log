# Mo2 LOG Android - validacao nativa

Este roteiro valida o APK Kotlin nativo antes de um treino real. O aplicativo nao usa WebView nem depende do backend.

## Preparar um emulador

1. No Android Studio, abra Tools > Device Manager.
2. Crie ou inicie um Pixel recente com API 35 ou 36.
3. Aguarde a tela inicial do Android antes de instalar o APK.

## Gerar e verificar o APK

```powershell
cd C:\Users\CDP\OneDrive\Documentos\Projeto\MoMo-Log\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
```

O artefato esperado e:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Escolher o dispositivo

```powershell
$adb='C:\Users\CDP\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
```

Quando houver emulador e celular conectados ao mesmo tempo, passe `-s SERIAL` nos comandos seguintes.

## Instalar preservando dados

```powershell
& $adb -s emulator-5554 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s emulator-5554 shell am force-stop br.com.mo2log.mobile
& $adb -s emulator-5554 shell monkey -p br.com.mo2log.mobile -c android.intent.category.LAUNCHER 1
```

`install -r` atualiza o APK sem limpar `SharedPreferences`. Nao use `pm clear` em um aparelho com historico real.

## Smoke test visual e funcional

- O Dashboard abre sem tela branca ou crash.
- Inicio, Treino, Corrida e Mais exibem icone, texto e estado selecionado.
- O menu inferior fica acima da barra de gestos/botoes do Android.
- O botao Voltar retorna de Historico/Perfil para Mais e depois para Inicio.
- Uma serie pode ser marcada, editada e mantida apos fechar e abrir o app.
- O descanso nao duplica e emite o aviso ao terminar.
- Uma corrida abre o countdown, atualiza velocidade/tempo e preserva a etapa ativa.
- O Historico agrupa o treino do dia e permite editar series, corridas e velocidades das etapas.
- O Perfil exporta um JSON e a versao exibida corresponde ao APK instalado.
- A interface nao corta textos em uma tela de aproximadamente 390 x 844.

## Diagnostico nativo

Limpe os logs antes de abrir o app:

```powershell
& $adb -s emulator-5554 logcat -c
& $adb -s emulator-5554 shell monkey -p br.com.mo2log.mobile -c android.intent.category.LAUNCHER 1
& $adb -s emulator-5554 logcat -d AndroidRuntime:E ActivityTaskManager:I '*:S'
```

Procure por `FATAL EXCEPTION`, `ANR` ou falha ao iniciar `br.com.mo2log.mobile.MainActivity`.

## Aparelho fisico

Depois do smoke test no emulador, repita a instalacao usando o serial do telefone:

```powershell
& $adb -s SERIAL_DO_TELEFONE install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

Abra o app e confirme o historico existente antes de registrar um treino novo.
