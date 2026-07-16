# Mo2 LOG Android nativo

Aplicativo pessoal em Kotlin e Android Views. Ele funciona no celular sem PC, servidor, login ou backend. A internet e usada somente para carregar as imagens remotas do catalogo; treinos, corridas e preferencias continuam locais.

## Funcionalidades

- Dashboard semanal e check-in de prontidao.
- Musculacao com series editaveis, descanso, midia, substituicoes e resumo.
- Corrida guiada por etapas com countdown, velocidade, pace e avisos de voz.
- Historico agrupado de treinos e edicao das etapas de corrida.
- Catalogo com 324 exercicios, busca, favoritos e alternativas.
- Estatisticas, metas, coach, editor de plano e backup JSON.

## Dados locais

O estado pessoal fica em `SharedPreferences`, no arquivo `mo2log_native`. Instalar uma nova versao com `adb install -r` preserva esses dados. Desinstalar o aplicativo ou limpar seu armazenamento pelo Android remove o historico local.

Use Perfil > Copiar backup JSON antes de uma limpeza ou troca de aparelho.

## Build

Requisitos:

- Android Studio com JDK 17.
- Android SDK 35.
- `JAVA_HOME` apontando para o JBR do Android Studio.

No PowerShell:

```powershell
cd C:\Users\CDP\OneDrive\Documentos\Projeto\MoMo-Log\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
```

APK gerado:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Instalar sem apagar dados

```powershell
& 'C:\Users\CDP\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
& 'C:\Users\CDP\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

## Estrutura principal

```text
app/src/main/java/br/com/mo2log/mobile/
|-- MainActivity.kt
|-- sync/HealthConnectSyncContract.kt
`-- ui/
    |-- Mo2Components.kt
    |-- Mo2DesignTokens.kt
    |-- Mo2DragHandleView.kt
    |-- Mo2Drawables.kt
    |-- Mo2HistoryChartView.kt
    `-- Mo2NavIconView.kt
```

`MainActivity.kt` preserva a arquitetura atual de Views programaticas. Os arquivos em `ui/` concentram tokens, estados, componentes e icones nativos reutilizaveis.

## Validacao

O roteiro de emulador, aparelho fisico, logcat e smoke test fica em [`docs/product/android-emulator-validation.md`](../../docs/product/android-emulator-validation.md).

## v10.2.0

- Consolida cores, tipografia, raios, cards e barras do sistema conforme a referencia do Figma.
- Substitui marcadores genericos do menu inferior por icones Android nativos e acessiveis.
- Mantem a navegacao acima da barra do sistema e melhora o comportamento do botao Voltar.
- Separa a versao do APK da versao do plano para preservar o ponto atual do treino em releases visuais.

## v10.3.0

- Historico inclui calendario mensal, filtros de 30/90 dias e selecao por dia.
- Recordes pessoais consolidam cargas, e1RM estimado, distancia e pace.
- Registros com pace impossivel sao sinalizados e nao entram nos recordes pessoais.
- Grafico nativo mostra oito semanas de volume de musculacao e corrida.
- Nenhuma migracao destrutiva e necessaria; backups anteriores continuam aceitos.

## v10.4.0

- Previsao de 5 km combina corridas validas, meta pessoal, confianca e tendencia recente.
- Sem amostras reais, o app mostra uma referencia inicial baseada no treino de ritmo da semana.
- Coach adaptativo usa RPE, prontidao, consistencia e evolucao prevista antes de sugerir alteracoes.
- Distancia semanal usa os quilometros registrados em cada corrida concluida.
- Voz pode ser desativada, testada ou repetida durante a corrida e inclui alertas em 30 e 10 segundos.
- Registros com duracao impossivel permanecem salvos, mas nao contaminam previsoes ou recomendacoes.

## v10.5.0

- Redesenha a aba Treino conforme a referencia do Figma sem remover registro, timer, GIFs, substituicoes ou coach.
- Checkboxes de series podem ser desmarcadas e mantem carga/repeticoes para uma nova conclusao.
- Edicoes posteriores a conclusao sincronizam o checklist e o log usado por historico, volume e estatisticas.
- Rolagem permanece no mesmo ponto depois de concluir uma serie.
- Progresso e proximo exercicio usam o primeiro item realmente pendente, inclusive quando o treino e feito fora de ordem.
- Exercicios concluidos ficam verdes e podem ser reordenados com gesto de segurar por tres segundos e arrastar.
- Descanso ganha `-30`, `+30`, timer branco e alerta sonoro confiavel ao finalizar.

## v10.6.0

- Move GIF, descricao, cuidados tecnicos e metadados do catalogo para o bloco Exercicio Atual.
- Remove o painel separado Detalhe do Exercicio da aba Treino para reduzir redundancia.
- Mantem atalhos de equipamento indisponivel, abertura no catalogo, troca de exercicio, registro de series e conclusao do treino.
- Exibe aviso compacto quando o exercicio planejado ainda nao tem midia vinculada no catalogo.

## v10.7.0

- Aba Corrida destaca o status semanal com checkbox preenchido, treino rodando e resumo das etapas.
- Corrida ativa mostra progresso da etapa atual com percentual e distancia feita.
- Historico de corrida exibe duracao e notas por etapa.
- Editor de etapa permite alterar nome, distancia, velocidade e observacao.
- Totais da corrida sao recalculados automaticamente depois da edicao das etapas.
