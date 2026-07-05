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

## v8.3.0

Replica os modulos da versao web como telas nativas locais: dashboard, treino, corrida, historico, stats, exercicios, metas, coach e perfil.

## v8.4.0

A aba Exercicios inclui catalogo amplo de academia com animacao nativa de execucao, descricao e alternativas por grupo muscular.

## v8.5.0

A aba Exercicios agora usa o catalogo importado com 324 exercicios e links remotos do Free Exercise DB para mostrar frames de execucao, alem de busca, filtros, detalhes tecnicos e alternativas.

## v8.6.0

Atualizacao visual da interface nativa mantendo a paleta verde/escura: novo header, navegacao com titulo de secao, home com atalhos, cards mais compactos e contexto melhor nas abas Treino e Exercicios.

## v8.7.0

UX pessoal para uso rapido na academia: favoritos no catalogo, atalhos na tela inicial, repetir ultima serie, progressao rapida de carga e avanco automatico para o proximo exercicio.

## v8.8.0

Catalogo avancado para uso pessoal: ocultar/restaurar exercicios com midia problematica, filtro Ocultos, substituto preferido por exercicio e status resumido da midia remota.

## v8.9.0

Redesign completo da navegacao nativa: menu inferior fixo com Home, Treino, Corrida e Mais, central Mais para telas secundarias e cabecalhos contextuais mantendo a paleta verde/escura.
