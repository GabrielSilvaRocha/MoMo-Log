# Mo2 LOG v8.1.0 - Android Offline Gym Mode

Objetivo: usar o Mo2 LOG na academia mesmo fora de casa, sem Wi-Fi da sua rede e com o computador desligado.

## O que ja funciona

- Tela Academia offline dentro do frontend.
- Entrada de login offline.
- Registro local de exercicio, serie, reps, carga, RIR, RPE e observacao.
- Exportacao JSON dos registros salvos no celular.
- Scaffold Android WebView para gerar APK que abre o build local do app.

## Caminho recomendado para treinar agora

1. Abra o app uma vez quando estiver com acesso ao frontend.
2. Entre em Usar modo academia offline.
3. Abra Academia offline.
4. Registre as series durante o treino.
5. Ao final, use Copiar exportacao para guardar os dados.

## Caminho APK

Este caminho dispensa PC ligado na academia.

1. Gere o build do frontend:

```bash
cd frontend
npm run build
```

2. Copie frontend/dist para:

```text
mobile/android/app/src/main/assets/mo2log
```

3. Abra mobile/android no Android Studio.

4. Gere o APK em Build > Build Bundle(s) / APK(s) > Build APK(s).

5. Instale o APK no Android.

O APK abre diretamente:

```text
file:///android_asset/mo2log/index.html?view=offline-workout
```

## Limite atual

Os dados ficam locais no Android/localStorage. Ainda nao ha sincronizacao automatica com o backend quando voce volta para casa. A proxima evolucao natural e criar uma importacao/sync dos JSONs offline para sessoes reais do backend.

## Alternativa cloud

Para ter o Mo2 LOG completo fora de casa, com historico centralizado e sem depender do PC, publique frontend, backend e banco em cloud com HTTPS. O modo offline continua util como fallback quando a internet da academia falhar.
