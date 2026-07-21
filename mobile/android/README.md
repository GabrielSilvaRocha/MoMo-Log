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

## v10.8.0

- Home recebe Central Pessoal com resumo da semana, tarefas do dia, saude dos dados e ultima atividade.
- Acoes rapidas deixam Treino, Corrida, Historico e Backup acessiveis com um toque.
- Proximo passo do dia passa a considerar check-in, treino planejado, corrida, backup e registros ja salvos.

## v10.9.0

- Historico recebe cabecalho V11 com resumo dos filtros atuais.
- Filtros ativos ficam descritos em texto para evitar leitura errada de periodos parciais.
- Atalhos de atividades e backup ficam antes de calendario, recordes, graficos e lista editavel.

## v11.0.0

- Versao estavel pessoal offline com Home, Treino, Corrida, Historico e Backup integrados.
- Cockpit e Home usam identidade V11 sem apagar dados locais existentes.
- Central Pessoal mostra proximo passo, ultima atividade e status de backup.

## v11.1.0

- Normaliza navegacao e selecoes persistidas na abertura para evitar estados invalidos depois de atualizacoes.
- Backup `personal_backup_v2` preserva os tipos originais das preferencias locais.
- Importacao valida o arquivo e mostra a quantidade de series, treinos e corridas antes da confirmacao.
- Cria uma copia automatica antes de restaurar e oferece `Desfazer ultima importacao` no Perfil.
- Mantem compatibilidade com backups `personal_backup_v1` e formatos legados.

## v11.2.0 a v11.4.0

- Central de Evolucao compara semana atual e anterior, exibe volume muscular e progressao sugerida.
- Corrida adaptativa usa pace valido, RPE, prontidao e previsao de 5 km.
- Corridas podem ser reagendadas, restauradas para a data original ou recuperadas hoje.

## v11.5.0 a v11.7.0

- Catalogo mostra tamanho do cache e prepara antecipadamente as midias do treino atual.
- Perfil verifica integridade das colecoes, idade do backup e quantidade de registros.
- Relatorio pessoal compara musculacao e corrida e pode ser copiado localmente.

## v11.8.0 e v11.9.0

- Texto ampliado e movimento reduzido ficam disponiveis no Perfil.
- Catalogo usa carregamento progressivo de resultados para abrir com menos trabalho na interface.
- `Mo2ProgressEngine` adiciona logica testavel para tendencia, consistencia, integridade e equilibrio muscular.
- Suite JUnit passa a validar o motor de evolucao no build Android.

## v12.0.0

- Consolida Home, Treino, Corrida, Historico, Estatisticas, Coach, Catalogo e Perfil na experiencia V12.
- Mantem dados, plano em andamento, backup e funcionamento pessoal local-first.
- Android usa versionCode 1200 e versionName 12.0.0.

## v12.0.1

- Remove o header global de Inicio, Corrida e Mais sem alterar os demais fluxos.
- Dashboard Semanal mostra volume acumulado, meta de volume e progresso semanal.
- Indicadores ficam distribuidos em duas linhas para evitar textos comprimidos.
- Android usa versionCode 1201 e versionName 12.0.1.

## v12.1.0

- Modal de alternativas passa a ter rolagem funcional, filtros 2x2 e botoes estaveis.
- Alerta de descanso usa voz e foco de audio temporario para priorizar a notificacao sobre musica externa.
- Exercicios podem ser reordenados apos um segundo e o Ajuste Inteligente fica abaixo do Exercicio Atual.
- Carga zero fica vazia na edicao e pesos disponiveis podem ser personalizados por exercicio.
- Recomendacoes respeitam pesos inteiros ou decimais configurados localmente.
- Exercicios unidos por `ou` sao separados com migracao compativel dos planos e checklists existentes.
- Android usa versionCode 1210 e versionName 12.1.0.

## v12.2.0

- Home remove o header antigo e destaca `Essa Semana` com Be Vietnam Pro Regular.
- Dashboard semanal adota dois aneis de progresso para musculacao e corrida.
- Metricas de series, volume, distancia e tempo ativo ganham leitura visual compacta.
- Android usa versionCode 1220 e versionName 12.2.0.

## v12.3.0

- Home recebe carrossel quadrado com os sete treinos da semana e fotos locais.
- Carrossel abre o fluxo correspondente, inicia no dia atual e permite gesto lateral, seta e indicadores.
- Dashboard recebe titulo interno e preserva os indicadores semanais abaixo do carrossel.
- Estado do slide e preferencias de movimento reduzido sao respeitados localmente.
- Android usa versionCode 1230 e versionName 12.3.0.

## v12.3.1

- Remove os cards antigos `MO2 LOG V12` e `HOJE` da Home.
- Agenda semanal recebe seletor de dias, datas reais, progresso e detalhes animados.
- Musculacao e corrida possuem indicadores proprios e dias hibridos exibem estado parcial.
- Acoes da agenda abrem diretamente o plano de musculacao, corrida ou coach correspondente.
- Android usa versionCode 1231 e versionName 12.3.1.
