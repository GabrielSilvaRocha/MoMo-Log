# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

## Estado atual

- Android nativo pessoal: v12.0.0.
- Dados locais preservados em `SharedPreferences`, sem depender do PC ou de um backend.
- Musculacao, corrida guiada, catalogo, historico editavel, estatisticas, metas, coach e backup JSON ativos.
- Redesign do Figma consolidado na branch `feature/android-figma-redesign`; `main` permanece sem merge direto.

## Proximas entregas

- v12.1.0 - Ampliacao dos testes de interface e diagnostico de desempenho em aparelhos reais.
- v12.2.0 - Periodizacao pessoal de musculacao por blocos e semanas de deload.
- v12.3.0 - Modo prova de 5 km, aquecimento guiado e estrategia de ritmo.
- v12.4.0 - Comparacoes mensais e metas pessoais de longo prazo.
- v13.0.0 - Nova consolidacao da experiencia pessoal offline.

## v8.1.2 - Android Offline Auto Login

Esta versao remove o erro failed to fetch no APK offline:

- APK cria sessao offline automaticamente quando abre view=offline-workout.
- Usuario nao precisa tocar em Demo Local nem fazer login online.
- Android versionCode atualizado para 812.

## v8.1.1 - Android WebView White Screen Fix

Esta versao corrige o APK Android que podia abrir em tela branca:

- WebView troca file:// por WebViewAssetLoader em https://appassets.androidplatform.net.
- AndroidX WebKit foi adicionado ao projeto Android.
- Build debug gera APK corrigido em mobile/android/app/build/outputs/apk/debug/app-debug.apk.
- Documento docs/product/android-emulator-validation.md descreve a validacao no emulador antes do celular.

## v8.1.0 - Android Offline Gym Mode

Esta versao prepara o uso na academia fora de casa, com PC desligado:

- Nova tela Academia offline registra series diretamente no armazenamento local do Android.
- Login permite entrar em modo offline quando a API nao esta disponivel.
- Frontend ganhou manifest, service worker e base relativa para PWA/APK WebView.
- Scaffold Android WebView carrega o build local em file:///android_asset/mo2log/index.html.
- Documento docs/product/android-offline-gym-mode.md explica os caminhos PWA, APK e cloud.

## v8.0.1 - Android Local Network Ready

Esta versao permite usar o app no navegador do Android pela mesma rede Wi-Fi do PC:

- Frontend troca automaticamente a API localhost pelo host aberto no navegador do celular.
- Backend em desenvolvimento aceita origens de rede local nas faixas 192.168.x.x, 10.x.x.x e 172.16-31.x.x.
- Documento docs/product/android-local-network.md traz o passo a passo para abrir no smartphone.

## v8.0.0 - Next Workout Ready

Esta versao deixa o Mo2 LOG pronto para abrir no proximo treino:

- Nova rota /api/v1/training-sessions/next-ready encontra a proxima sessao executavel dos proximos 14 dias.
- Tela de execucao mostra o proximo treino recomendado com checklist, aquecimento e botao de carregamento automatico.
- Registro de series, descanso automatico, sugestao de carga e troca de exercicio ficam no mesmo fluxo de uso.
- Documento docs/product/next-workout-ready.md descreve o roteiro pratico para usar no treino.

## v7.2.0 — Android Health Connect App Readiness

Esta versão prepara o app Android nativo:

- Mobile sync agora descreve app Kotlin, Jetpack Compose, WorkManager e permissões Health Connect.
- Nova rota /api/v1/mobile-sync/android-plan expõe módulos, dependências e payload esperado.
- Painel Deploy exibe grupos de permissões e janelas de sincronização mobile.
- Documentação docs/mobile/health-connect-android.md e scaffold mobile/android foram adicionados.

## v7.1.0 — Cloud Demo Readiness

Esta versão prepara a publicação pública do Mo² LOG:

- Rota /api/v1/ops/cloud-demo-readiness com stack recomendada, variáveis, smoke tests e rollback.
- Painel Deploy exibe cloud demo, domínios, serviços e endpoints de validação.
- Docker Compose de produção atualizado para fallback APP_VERSION 7.1.0.
- Documentação docs/deployment/cloud-demo.md e manifestos exemplo para Render e Vercel.

## v7.0.0 — Portfolio Release

Esta versão consolida as entregas planejadas até v7.0.0:

- Dashboard híbrido com score, foco semanal, recuperação e mix força/corrida.
- Rota de readiness para Health Connect / Samsung Health com estratégia de sincronização e mapeamento de dados.
- Painel Deploy ampliado com checklist de portfólio, roteiro de demo e screenshots-alvo.
- Documento docs/product/portfolio-demo.md com pacote de apresentação do projeto.

## v6.8.0 — Mobile Sync Readiness

Esta versão prepara a futura integração mobile:

- Contrato /api/v1/mobile-sync/readiness descreve plataformas, permissões e mapeamento de dados.
- Estratégia import-first prioriza Health Connect e mantém registro manual como fallback.
- Política de conflitos prevê revisão manual para evitar duplicidade de corridas.
- Painel Deploy passa a exibir o roteiro mobile.

## v6.7.0 — Hybrid Dashboard Evolution

Esta versão evolui o dashboard:

- Score híbrido combina consistência, força, corrida e volume semanal.
- Mix semanal separa força, corrida e recuperação planejadas/concluídas.
- Foco da semana orienta a próxima ação com base no estado atual.
- Métricas principais passam a destacar evolução híbrida em vez de apenas volume bruto.

## v6.6.0 — Running Plan Progression Preferences

Esta versão evolui o Running Coach:

- Objetivo de corrida salva treinos por semana, estilo de progressão e dia do treino longo.
- Gerador cria semanas com 2 a 5 sessões, respeitando dias disponíveis sempre que possível.
- Progressões conservadora, equilibrada e agressiva ajustam volume, tiros e ritmo.
- Tela Corridas permite configurar as preferências antes de gerar o plano.

## v6.5.0 — Race Distance Forecasts

Esta versão evolui a inteligência de corrida:

- Previsão de tempo para qualquer distância-alvo informada.
- Compatibilidade mantida com a previsão clássica de 5 km.
- Tela Inteligência permite alternar rapidamente entre 5 km, 10 km e meia maratona.
- Forecast usa pace recente e ajusta a projeção pela distância-alvo.

## v6.4.0 — Strength Load Progression

Esta versão evolui a execução de musculação:

- Sugestão de próxima carga por exercício com base no histórico de séries.
- Regras simples usando carga recente, RIR e RPE para aumentar, manter ou reduzir.
- Tela de execução exibe recomendação de carga junto do exercício planejado.
- API dedicada para consultar progressão de carga sem criar novas tabelas.

## v6.3.0 — Custom Workout Templates

Esta versão evolui o módulo Templates:

- Criação de templates personalizados a partir da biblioteca de exercícios.
- Configuração de séries, repetições, descanso e observações por exercício.
- Arquivamento de templates ativos sem remover histórico ou sessões já criadas.
- Tela Templates passa a centralizar criação, seleção e agendamento.

## v6.2.0 — Running Coach Auto Progression

Esta versão evolui a execução guiada do módulo Corridas:

- Etapas avançam automaticamente quando a distância ou o tempo chega a zero.
- Backend conclui o log da etapa, retorna a próxima etapa e encerra a sessão na etapa final.
- Painel da esteira permite concluir a etapa atual manualmente quando necessário.
- Running Coach passa a registrar a execução como fluxo contínuo, sem depender de cliques entre blocos.

## v6.1.1 — Running Coach Execution Hotfix

Esta versão corrigiu e melhorou a execução guiada do módulo Corridas:

- Timer de 5 segundos antes de iniciar a primeira etapa.
- Execução baseada em distância, com km regressivos quando a etapa possui distância planejada.
- Tempo restante recalculado automaticamente quando a velocidade da esteira muda.
- Botões `+` e `-` continuam ajustando a velocidade em 0,1 km/h.
- Pace exibido a partir da velocidade atual da esteira.
- Etapas de descanso continuam controladas por tempo.
- Teste operacional de versão estabilizado para não quebrar a cada release.

## Rodando localmente

```bash
docker compose down
docker compose up --build
```

Em outro terminal:

```bash
docker compose exec backend alembic upgrade head
docker compose exec backend python -m pytest -q -vv
```

Frontend:

```text
http://localhost:5173
```

Backend:

```text
http://localhost:8000/docs
```

## Login local

Use o botão **Entrar como Demo Local**. Não há senha demo versionada no repositório.

## v8.1.3 - Android APK sem cache legado

- Corrige o menu do shell Android para expor a entrada Academia offline.
- Desativa service worker no host interno do APK e remove caches legados do WebView.
- Regera o APK como versao 8.1.3 para evitar bundle preso em instalacoes antigas.


## v8.1.4 - Android Offline-Only APK

- APK Android fica restrito ao modo Academia offline.
- Menu online e rotas que dependem do backend ficam escondidos no WebView Android.
- Reduz o risco de failed to fetch durante treino fora de casa.


## v8.2.0 - App Android nativo pessoal

- Substitui o APK WebView por uma MainActivity Android nativa em Kotlin.
- Remove dependencia de backend, login, servidor local e conceitos online/offline no app mobile.
- Inclui programa A/B/C/D inicial, registro local de series, historico e exportacao via area de transferencia.
- Adiciona logo nativo ao app Android.


## v8.3.0 - App Android nativo completo

- Replica os principais modulos da versao web no Android nativo pessoal.
- Adiciona abas locais: Inicio, Treino, Corrida, Historico, Stats, Exercicios, Metas, Coach e Perfil.
- Mantem todos os dados no celular com SharedPreferences e exportacao JSON pela area de transferencia.
- Mantem programa A/B/C/D embutido e adiciona corrida de esteira, estatisticas, metas e relatorios locais.


## v8.4.0 - Catalogo nativo de exercicios

- Aba Exercicios ganha catalogo amplo de exercicios de academia.
- Cada exercicio inclui animacao nativa, descricao, alternativas e registro rapido.
- O app Android passa para versionCode 840 / versionName 8.4.0.

## v8.5.0 - Catalogo de exercicios com midia por link

- A aba Exercicios passa a usar o catalogo importado do pacote `catalogo_exercicios_musculacao_links_repo.zip`.
- O APK inclui 324 exercicios com grupo muscular, equipamento, nivel, musculos primarios/secundarios, cuidados tecnicos e alternativas.
- As imagens de execucao sao carregadas por link do Free Exercise DB e alternadas no app como frames de demonstracao.
- Adiciona busca por nome, musculo ou equipamento e filtros por grupo muscular.
- O app Android passa para versionCode 850 / versionName 8.5.0.

## v8.6.0 - Interface Android mais bonita e intuitiva

- Remove do roadmap imediato as entregas de compartilhamento publico e camada social.
- Mantem a paleta verde/escura do Mo2 LOG, mas refina header, navegacao, cards, botoes e campos.
- Inicio passa a destacar resumo do dia, atalhos para Treino, Catalogo e Corrida, e o proximo treino.
- Aba Treino ganha cartao de contexto para orientar o registro durante a academia.
- Aba Exercicios ganha resumo da biblioteca antes da busca e dos filtros.
- O app Android passa para versionCode 860 / versionName 8.6.0.

## v8.7.0 - UX pessoal com favoritos e registro rapido

- Catalogo de exercicios permite adicionar/remover favoritos e filtrar apenas favoritos.
- Inicio mostra atalhos para exercicios favoritos quando houver favoritos salvos.
- Tela de treino lembra a ultima serie do exercicio selecionado.
- Registro rapido permite repetir a ultima serie ou salvar com +2,5 kg.
- Ao salvar uma serie dentro do treino, o app avanca automaticamente para o proximo exercicio.
- O app Android passa para versionCode 870 / versionName 8.7.0.

## v8.8.0 - Catalogo avancado

- Catalogo permite ocultar exercicios/midias problematicas sem apagar o exercicio do app.
- Filtro Ocultos permite revisar e restaurar itens ocultados.
- Alternativas podem ser marcadas como substituto preferido por exercicio.
- Detalhe do exercicio mostra um resumo da midia remota disponivel.
- Favoritos e ocultos convivem de forma segura: ocultar remove o item dos favoritos.
- O app Android passa para versionCode 880 / versionName 8.8.0.

## v8.9.0 - Redesign Android com menu inferior

- Navegacao principal passa a usar menu inferior fixo com Home, Treino, Corrida e Mais.
- Telas secundarias agora ficam organizadas dentro da central Mais: Exercicios, Historico, Stats, Metas, Coach e Perfil.
- Cabecalho de cada tela ganhou contexto curto para orientar o uso durante a academia.
- A paleta verde/escura atual foi mantida, com layout mais direto para uso no smartphone.
- O app Android passa para versionCode 890 / versionName 8.9.0.

## v8.9.1 - Area segura da navegacao Android

- Menu inferior passa a respeitar a barra de navegacao do Android via WindowInsets.
- Botoes Home, Treino, Corrida e Mais ficam acima dos botoes virtuais ou area de gestos do sistema.
- Conteudo superior tambem recebe margem segura para evitar sobreposicao com a status bar.
- O app Android passa para versionCode 891 / versionName 8.9.1.

## v9.0.0 - Treino premium

- Tela Treino ganha painel de sessao com progresso do exercicio atual, series do dia e botoes Anterior/Proximo.
- Registro de serie inicia automaticamente um timer de descanso com base no descanso planejado do exercicio.
- Timer permite iniciar manualmente, somar +30s e parar quando necessario.
- Painel de registro permite editar a ultima serie do exercicio e desfazer a ultima serie salva.
- Catalogo salva frames remotos em cache local para acelerar midias ja abertas.
- Aba Exercicios mostra quantos frames estao em cache e permite limpar o cache de imagens.
- O app Android passa para versionCode 900 / versionName 9.0.0.

## v9.1.0 - Midia de execucao no treino

- Aba Treino passa a mostrar a midia de execucao do exercicio selecionado.
- Exercicios planejados com nomes genericos usam aliases para encontrar a melhor correspondencia no catalogo.
- O painel de midia no Treino reaproveita o cache local de frames criado na v9.0.0.
- Botao Abrir no catalogo leva direto ao detalhe completo do exercicio sugerido.
- O app Android passa para versionCode 910 / versionName 9.1.0.

## v9.2.0 - Plano pessoal hibrido

- App passa a refletir a rotina noturna: musculacao primeiro e corrida depois.
- Segunda fica sem musculacao e dedicada ao treino forte de corrida para 5 km.
- Musculacao fica em 3 dias: terca Treino A, quinta Treino B pernas/core e sabado Treino C costas/biceps.
- Aba Corrida mostra a semana de 5 km com tiros, corrida leve, ritmo e longo leve.
- Home ganha card do plano semanal hibrido com atalhos para Treino e Corrida.
- O app Android passa para versionCode 920 / versionName 9.2.0.

## v9.3.0 - Corrida guiada 5 km

- Aba Corrida passa a abrir com o bloco Essa Semana e os 5 treinos do ciclo atual.
- Cada treino mostra checkbox de conclusao e botao Treino concluido para registrar treino feito sem o app.
- Botao Planejamento completo exibe todos os treinos das 6 semanas ate a meta de 5 km.
- Ao tocar no treino, o card expande com fases, distancias, velocidades e botao Iniciar.
- O treino ativo tem contagem inicial de 5 segundos, cronometro regressivo, km restantes ate a proxima fase e ajuste de velocidade por botoes -/+.
- Coach por voz avisa quando faltam 30 segundos para trocar de fase e informa a proxima distancia/velocidade.
- O app Android passa para versionCode 930 / versionName 9.3.0.

## v9.4.0 - Checklist de series e editor local

- Aba Treino troca o formulario antigo por uma lista de series com carga, repeticoes e checkbox de conclusao.
- O app nao muda de exercicio apos uma unica serie; so avanca quando todas as series planejadas do exercicio forem concluidas.
- Cronometro de descanso fica abaixo do exercicio atual e toca som de notificacao quando termina.
- Botao + adiciona nova serie e o gesto de deslizar da direita para a esquerda remove a serie com destaque vermelho durante o movimento.
- Botao Trocar por recomendado usa o catalogo para substituir o exercicio por alternativa sugerida.
- Ao concluir o treino, aparece popup com os exercicios feitos no dia e botao OK.
- Tela Plano permite editar treinos, exercicios e ajustes basicos da corrida direto no celular.
- O app Android passa para versionCode 940 / versionName 9.4.0.

## v9.5.0 - Historico avancado e troca animada

- Aba Historico ganha filtros por data inicial, data final, tipo e busca por treino/exercicio/observacao.
- Historico mostra metricas filtradas de series, volume, corridas, distancia, maior carga e dias registrados.
- Painel Evolucao por exercicio resume series, volume, melhor carga e variacao de carga por movimento.
- Listas de musculacao e corrida passam a respeitar os filtros aplicados.
- Botao Trocar por recomendado abre popup com alternativas para escolher antes de substituir.
- Popup de troca usa animacao suave de entrada para deixar o movimento menos brusco.
- O app Android passa para versionCode 950 / versionName 9.5.0.

## v9.6.0 - Backup pessoal JSON

- Perfil ganha painel de backup pessoal para copiar todos os dados locais em JSON.
- Backup inclui series, corridas, plano editado, favoritos, ocultos, substitutos, metas e ajustes.
- Importacao pode ser feita por JSON colado ou direto do clipboard.
- Importacao tambem aceita o formato antigo de exportacao basica.

## v9.7.0 - Resumo final completo

- Popup de conclusao de treino mostra series, exercicios feitos, volume total, RPE medio e melhor carga.
- Resumo inclui as corridas registradas no mesmo dia.
- Popup ganha botao Copiar para salvar o resumo no clipboard.
- O app Android passa para versionCode 970 / versionName 9.7.0.

## v9.8.0 - Ajustes inteligentes de carga, volume e ritmo

- Aba Treino ganha painel de ajuste inteligente com proxima carga e volume sugerido.
- Sugestao de carga usa historico recente, faixa de reps e RPE para subir, manter ou reduzir com cautela.
- Botoes Aplicar carga e Ajustar series atualizam apenas as series pendentes do exercicio atual.
- Aba Corrida ganha ajuste de ritmo com microalteracoes de velocidade e distancia conforme progresso.
- Stats passa a exibir Coach Inteligente com proximos ajustes de musculacao e corrida.
- Popup final de treino inclui as recomendacoes para o proximo ajuste.
- O app Android passa para versionCode 980 / versionName 9.8.0.

## v9.9.0 - Polimento de uso real na academia e esteira

- Aba Treino ganha Modo Academia com exercicio atual, proxima serie, descanso e proximo movimento.
- O app mantem a tela ativa enquanto a aba Treino estiver aberta.
- Modo Academia adiciona atalhos Carga e Descanso para reduzir toques durante o treino.
- Aba Corrida ganha Modo Esteira com distancia, duracao, inclinacao sugerida e velocidades por fase.
- Corrida ativa mostra a velocidade que deve estar configurada na esteira em tempo real.
- O app mantem a tela ativa durante corrida iniciada pelo app.
- O app Android passa para versionCode 990 / versionName 9.9.0.

## v10.0.0 - Versao pessoal madura

- Home ganha Cockpit V10 com missao do dia, progresso semanal e prontidao.
- Cockpit mostra series da semana contra meta e corridas concluidas na semana atual.
- Checklist de continuidade acompanha musculacao, corrida, backup diario e meta semanal.
- Botao Backup no Cockpit copia o JSON pessoal e marca o backup do dia como concluido.
- Atalho principal muda conforme o dia: treino, corrida ou coach.
- Linha de prontidao orienta cautela quando o RPE recente esta alto ou quando o dia ja foi completo.
- O app Android passa para versionCode 1000 / versionName 10.0.0.

## v10.1.0 - Check-in diario de prontidao

- Home ganha Check-in Rapido com estados Verde, Amarelo e Vermelho.
- Check-in do dia alimenta a linha de prontidao do Cockpit V10.
- Checklist de continuidade passa a acompanhar se o check-in diario foi feito.
- Coach e Insights passam a considerar a prontidao registrada no dia.
- Verde orienta seguir o plano com progressao controlada; Amarelo reduz ambicao; Vermelho prioriza recuperacao.
- O app Android passa para versionCode 1010 / versionName 10.1.0.

## v10.2.0 - Consolidacao visual Android

- Design system alinhado aos tokens do Figma, incluindo cores de corrida/alerta, raios, tipografia e estados de interacao.
- Cards padrao passam a usar a superficie correta e as barras do sistema seguem a paleta do aplicativo.
- Bottom navigation ganha icones Android nativos para Inicio, Treino, Corrida e Mais, com selecao, ripple e area de toque consistentes.
- Botao Voltar retorna de telas secundarias para Mais e das abas principais para Inicio; durante uma corrida ativa pede confirmacao antes de sair.
- Versao do aplicativo e versao do plano ficam independentes, evitando reposicionar o treino em atualizacoes apenas visuais.
- Documentacao Android e roteiro de validacao foram atualizados para o aplicativo Kotlin nativo atual.
- O app Android passa para versionCode 1020 / versionName 10.2.0.

## v10.3.0 - Historico avancado e recordes

- Historico ganha calendario mensal navegavel com marcadores de musculacao e corrida.
- Toque em um dia aplica o filtro exato; atalhos permitem alternar entre 30 dias, 90 dias e todo o historico.
- Filtros avancados validam datas e impedem intervalos invertidos.
- Painel de recordes calcula maior carga, e1RM estimado, corrida mais longa e melhor ritmo acima de 1 km.
- Ritmos fora da faixa plausivel sao ignorados para que registros de teste ou duracoes incompletas nao virem recordes.
- Grafico Android nativo compara volume de musculacao e distancia corrida nas ultimas oito semanas.
- Recordes usam todo o historico; resumo, grafico e atividades continuam reagindo aos filtros ativos.
- A estrutura dos dados pessoais permanece inalterada e compativel com os backups existentes.
- O app Android passa para versionCode 1030 / versionName 10.3.0.

## v10.4.0 - Corrida adaptativa e previsao de 5 km

- Corrida ganha previsao de 5 km com meta editavel, pace, velocidade, confianca e tendencia.
- A previsao usa corridas reais com ritmo plausivel e recorre ao bloco de ritmo do plano enquanto faltam amostras.
- Coach de ritmo passa a considerar consistencia semanal, RPE, prontidao e tendencia da previsao.
- Corridas marcadas manualmente e registros de teste com ritmo impossivel nao alteram a adaptacao automatica.
- Resumo semanal passa a somar a distancia realmente salva, em vez da distancia total planejada do treino.
- Comandos de voz ganham controle liga/desliga, teste, repeticao da instrucao e aviso opcional aos 10 segundos.
- Voz anuncia inicio, 30 segundos, 10 segundos, troca de etapa, velocidade ajustada, pausa, retomada e conclusao.
- Numeros de distancia e velocidade passam a ser pronunciados em portugues brasileiro.
- Meta e configuracoes de voz usam preferencias aditivas e continuam protegidas pelo backup JSON atual.
- O app Android passa para versionCode 1040 / versionName 10.4.0.

## v10.5.0 - Treino fluido e redesign da musculacao

- Aba Treino adota a hierarquia visual do Figma com cabecalho compacto, resumo da sessao, exercicio atual e lista de exercicios com estados claros.
- Marcar ou desmarcar uma serie cria ou remove o respectivo registro local sem perder carga e repeticoes digitadas.
- Alteracoes de carga e repeticoes feitas depois da conclusao atualizam imediatamente o historico e o volume considerado.
- Concluir uma serie preserva a posicao vertical da tela e concluir um exercicio seleciona o primeiro exercicio ainda pendente.
- Progresso da sessao passa a usar exercicios realmente concluidos, independentemente da ordem escolhida.
- Exercicios concluidos ficam verdes e a lista pode ser reordenada ao segurar um item por tres segundos e arrasta-lo.
- Reordenacao migra os checklists salvos para que series, cargas e repeticoes continuem ligadas ao exercicio correto.
- Timer de descanso usa texto branco, controles de menos/mais 30 segundos e notificacao sonora em dois tons ao finalizar.
- Campos de series recebem cabecalhos fixos, checkbox nativa, estados responsivos e acoes sem quebra de texto.
- O app Android passa para versionCode 1050 / versionName 10.5.0.

## v10.6.0 - Exercicio atual sem redundancia

- Aba Treino passa a concentrar GIF, descricao de execucao, cuidados tecnicos, grupo muscular, equipamento e nivel diretamente no bloco Exercicio Atual.
- O antigo painel Detalhe do Exercicio foi removido do fluxo da tela para evitar repeticao de informacoes e rolagem desnecessaria.
- Atalhos de equipamento indisponivel e abertura no catalogo permanecem acessiveis no proprio contexto do exercicio atual.
- Quando o exercicio ainda nao tem midia vinculada ao catalogo, o app mostra um aviso compacto sem bloquear o registro das series.
- O app Android passa para versionCode 1060 / versionName 10.6.0.

## v10.7.0 - Corrida semanal e etapas editaveis

- Aba Corrida melhora o bloco Essa Semana com checkbox visual por treino, status Rodando/Feito/Pendente e resumo de etapas.
- Tela de corrida ativa passa a mostrar o avanco da etapa atual com percentual, distancia feita e distancia total da etapa.
- Historico de corrida mostra duracao estimada e observacao de cada etapa ao expandir o treino intervalado.
- Edicao de etapa passa a permitir alterar nome, distancia, velocidade e observacao, nao apenas velocidade.
- Ao editar uma etapa, o app recalcula distancia total, duracao, velocidade media, pace e quantidade de etapas do registro.
- O app Android passa para versionCode 1070 / versionName 10.7.0.

## v10.8.0 - Dashboard inicial inteligente

- Home ganha Central Pessoal com leitura de semana, tarefas do dia, saude dos dados locais e ultima atividade.
- Acoes rapidas passam a expor treino, corrida, historico e backup diretamente na tela inicial.
- Proximo passo do dia considera check-in, musculacao planejada, corrida planejada, backup e registros ja feitos.
- O app Android prepara a transicao para a serie 11 com foco em uso pessoal offline.

## v10.9.0 - Historico consolidado

- Historico ganha cabecalho V11 com resumo filtrado de treinos, volume e distancia corrida.
- Filtros ativos ficam explicitos para reduzir risco de interpretar dados incompletos como historico total.
- Atalhos de atividades e backup ficam no topo do Historico sem remover calendario, recordes, graficos ou edicao.
- Registros locais continuam editaveis e excluiveis no proprio aparelho.

## v11.0.0 - Estavel pessoal offline

- Mo2 Log passa para uma versao estavel pessoal com Home, Treino, Corrida, Historico e Backup integrados para uso offline.
- Cockpit e Home usam rotulos V11, mantendo dados locais existentes e sem migracao destrutiva.
- Central Pessoal destaca o que fazer agora, ultima atividade registrada e status de backup.
- Historico V11 reforca revisao, edicao e exportacao local antes de treinos futuros.
- O app Android passa para versionCode 1100 / versionName 11.0.0.

## v11.1.0 - Restauracao segura e estabilidade local

- A abertura normaliza aba, treino, exercicio e corrida selecionados para impedir indices invalidos depois de atualizacoes ou backups antigos.
- Sessoes de corrida vinculadas a um plano inexistente sao encerradas com seguranca sem afetar o historico ja salvo.
- Backup passa ao esquema `personal_backup_v2`, preservando explicitamente os tipos de `String`, `Boolean`, `Int`, `Long`, `Float` e conjuntos.
- Importacao valida origem, esquema e colecoes antes de gravar, mostra uma previa com totais e exige confirmacao.
- O app cria uma copia automatica dos dados atuais antes da importacao e permite desfazer a ultima restauracao pelo Perfil.
- Backups `personal_backup_v1` e formatos legados continuam aceitos com conversao compativel de tipos.
- O app Android passa para versionCode 1110 / versionName 11.1.0.

## v11.2.0 - Progressao e volume muscular

- Central de Evolucao compara series e volume dos ultimos sete dias com a semana anterior.
- Estatisticas mostram volume acumulado por grupo muscular nos ultimos 28 dias e sinalizam concentracoes.
- Progressao do exercicio atual combina carga sugerida, volume e justificativa usando o historico local.

## v11.3.0 - Corrida adaptativa consolidada

- Coach de corrida combina desempenho valido, RPE, prontidao e previsao de 5 km para ajustar ritmo e distancia.
- Corridas manuais ou registros sem pace plausivel nao distorcem a adaptacao automatica.
- Previsao e proxima decisao de corrida passam a aparecer tambem no Coach integrado.

## v11.4.0 - Reagendamento e recuperacao

- Cada corrida recebe uma data efetiva calculada a partir do inicio do ciclo.
- Treinos podem ser reagendados, devolvidos ao plano original ou recuperados hoje.
- A selecao automatica do treino do dia respeita datas personalizadas e pendencias.

## v11.5.0 - Catalogo preparado para o treino

- Catalogo mostra quantidade e tamanho do cache de midia.
- Acao Preparar treino baixa antecipadamente as midias dos exercicios do plano atual.
- Limpeza de cache permanece disponivel sem afetar historico, plano ou favoritos.

## v11.6.0 - Integridade local

- Perfil verifica colecoes JSON, quantidade de registros, idade do backup e tamanho do cache.
- Status Integro, Atencao ou Revisar orienta quando criar um novo backup.
- Backup tipado e reversao segura da v11.1 continuam preservados.

## v11.7.0 - Relatorio pessoal comparativo

- Relatorio V12 reune consistencia, series, volume, corrida e previsao de 5 km.
- Comparacao semanal pode ser copiada para o clipboard sem servidor ou conta externa.

## v11.8.0 - Acessibilidade e desempenho

- Perfil oferece texto ampliado e movimento reduzido em pop-ups.
- Calculos de evolucao usam janelas locais limitadas e cache de grupo muscular por exercicio.
- Catalogo renderiza 30 resultados por vez e permite carregar mais sem perder busca ou filtros.

## v11.9.0 - Testes automatizados

- Motor `Mo2ProgressEngine` separa tendencia, consistencia, integridade e equilibrio de volume da interface Android.
- Suite JUnit cobre crescimento, nova linha de base, limites de consistencia, backup antigo e concentracao muscular.

## v12.0.0 - Experiencia pessoal integrada

- Home, Treino, Corrida, Historico, Estatisticas, Coach, Catalogo e Perfil compartilham a identidade V12.
- Central de Evolucao integra musculacao e corrida sem remover os fluxos anteriores.
- Aplicativo continua pessoal, local-first e funcional sem PC ou backend; internet segue opcional para baixar midias.
- O app Android passa para versionCode 1200 / versionName 12.0.0.
