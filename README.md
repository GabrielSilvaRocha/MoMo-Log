# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

## Proximas entregas

- v8.2.0 - Sincronizacao/importacao dos treinos offline para o backend.
- v8.3.0 - Polimento mobile-first da tela de treino para uso na academia.
- v8.4.0 - Catalogo nativo de exercicios para uso offline no Android.
- v8.5.0 - Catalogo de exercicios importado com midia remota por link.
- v8.6.0 - Atualizacao visual da interface Android nativa.
- v8.7.0 - UX pessoal para favoritos, atalhos e fluxo de treino mais rapido.
- v8.8.0 - Catalogo avancado com ocultos, substitutos preferidos e status de midia.
- v8.9.0 - Redesign Android com menu inferior fixo e central Mais.
- v8.9.1 - Correcao da area segura do menu inferior no Android.
- v9.0.0 - Treino premium com timer, editar/desfazer serie, descanso e cache de imagens.
- v9.1.0 - Midia de execucao do exercicio selecionado dentro da aba Treino.
- v9.2.0 - Plano pessoal hibrido: musculacao 3x/semana e corrida para 5 km.
- v9.3.0 - Corrida guiada 5 km com semana atual, planejamento completo e fases tipo Runna.
- v9.4.0 - Checklist de series no treino, descanso sonoro e editor local do plano pessoal.
- v9.5.0 - Historico avancado com filtros, metricas, evolucao por exercicio e troca recomendada animada.
- v9.6.0 - Backup pessoal por exportacao/importacao JSON.
- v9.7.0 - Resumo final de sessao mais completo.
- v9.8.0 - Ajustes inteligentes de carga, volume e ritmo.
- v9.9.0 - Polimento de uso real na academia e esteira.
- v10.0.0 - Versao pessoal madura para uso continuo na academia.
- v10.1.0 - Check-in diario de prontidao para ajustar intensidade.

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
