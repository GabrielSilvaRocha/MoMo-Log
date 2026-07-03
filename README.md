# Mo² LOG

Mo² LOG é uma aplicação de treino híbrido para musculação e corrida.

## Proximas entregas

- v8.2.0 - Sincronizacao/importacao dos treinos offline para o backend.
- v8.3.0 - Polimento mobile-first da tela de treino para uso na academia.
- v8.4.0 - Assistente de ajustes entre series com base em RIR, RPE e carga.
- v8.5.0 - Compartilhamento publico de relatorios e snapshots de progresso.
- v8.6.0 - Camada social de evolucao, provas e comparativos.

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
