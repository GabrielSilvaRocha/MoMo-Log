# Roadmap do Mo² LOG

## v2.0.0 — MVP Consolidado

- Consolidar telas e fluxos principais.
- Consolidar Running Coach como fluxo principal de corrida.
- Priorizar corrida manual de esteira.
- Adicionar status de produto e checklist do MVP.

## v3.0.0 — Usuários e autenticação

- Login/cadastro.
- JWT.
- Perfil do usuário.
- Dados isolados por usuário.
- Configuração da academia.

## v4.0.0 — Inteligência e evolução

- Comparação planejado vs realizado.
- Insights por tendência.
- Detecção de baixa consistência.
- Evolução de volume e pace.

## v5.0.0 — Deploy e portfólio

- Deploy backend/frontend.
- Banco em cloud.
- README de portfólio.
- Prints e vídeo demonstrativo.

## v12.4.0 - Planejamento pessoal editavel

- Carrossel da Home abre no dia atual e usa as atividades reais configuradas para cada dia.
- Dias com musculacao e corrida alternam as duas atividades no mesmo quadro a cada cinco segundos.
- Atividades concluidas recebem borda verde no carrossel semanal.
- Vozes da corrida solicitam o mesmo foco de audio transitivo usado pelo alerta de descanso.
- Editor local permite criar, remover, editar e mudar o dia dos treinos de musculacao.
- Planejamento de corrida permite editar sessoes, dias e todas as etapas sem perder o plano original.
- Catalogo permite personalizar localmente titulo, descricao e links de GIF de cada exercicio.
- Android usa versionCode 1240 e versionName 12.4.0.

## v12.4.1 - Home essencial

- Saudacao pessoal varia de acordo com o horario local e usa emoji contextual.
- Home mantem somente carrossel semanal, Dashboard e Agenda da semana.
- Componentes redundantes deixam de ocupar a tela sem remover suas funcoes das demais areas do app.
- Slider, Dashboard, Agenda e navegacao inferior recebem o refinamento visual aprovado.
- Android usa versionCode 1241 e versionName 12.4.1.

## v12.4.2 - Agenda fiel ao plano

- Gerar o carrossel da Home somente a partir de musculacao e corrida realmente planejadas.
- Abrir no treino atual, no proximo dia planejado quando hoje estiver livre ou no ultimo treino quando a agenda da semana tiver terminado.
- Atribuir e persistir um card da Home para cada plano de musculacao, com edicao visual na pagina do plano.
- Migrar planos existentes sem apagar exercicios, dias, historico ou preferencias.
- Manter a identidade visual das corridas automatica.
- Android usa versionCode 1242 e versionName 12.4.2.

## v12.4.3 - Iconografia de corrida unificada

- Reutilizar a silhueta oficial do Dashboard em todos os pontos que representam corrida.
- Remover os desenhos alternativos da navegacao inferior e da Agenda semanal.
- Preservar as cores contextuais e o equilibrio visual em tamanhos compactos.
- Android usa versionCode 1243 e versionName 12.4.3.

## v12.4.4 - Substituicoes auditadas e midia local

- Priorizar alternativas equivalentes por grupo muscular, movimento, regiao e configuracao explicita.
- Excluir das trocas os exercicios que ja pertencem ao planejamento do mesmo dia.
- Permitir edicao manual dos alternativos na pagina de Exercicios.
- Renomear `Agachamento guiado` para `Agachamento no Smith` e migrar o plano salvo.
- Incorporar ao APK um GIF animado offline para o Agachamento no Smith.
- Android usa versionCode 1244 e versionName 12.4.4.

## v12.4.5 - Biblioteca de exercicios redesenhada

- Remover o cabecalho global redundante da aba Exercicios e adotar barra propria.
- Reorganizar busca, filtros, GIF, execucao, cuidados, alternativas e lista relacionada.
- Preservar favoritos, edicao manual, ocultacao, cache e preparo de midia em controles contextuais.
- Permitir adicionar um exercicio ao plano escolhido e registrar series diretamente da biblioteca.
- Validar filtros agrupados, passos de execucao e rotulos compactos com testes unitarios.
- Android usa versionCode 1245 e versionName 12.4.5.

## v12.4.6 - Troca de exercicio responsiva

- Remover o ranking e a verificacao repetitiva de duplicatas da thread visual.
- Preparar alternativas uma unica vez e reutiliza-las ao mudar o filtro do pop-up.
- Manter cache dos vinculos entre nomes planejados e itens do catalogo.
- Preservar a posicao da aba Treino depois de aplicar a substituicao.
- Cobrir exclusao por planejamento, equipamento, preferencia e limite com testes unitarios.
- Android usa versionCode 1246 e versionName 12.4.6.

## v12.4.7 - Redesign da sessao de musculacao

- Unificar plano, progresso e exercicio atual sem remover funcoes da sessao.
- Exibir GIFs na proporcao original, com altura responsiva e sem corte vertical.
- Compactar series, cronometro, ajuste inteligente e fila de exercicios.
- Substituir os pop-ups extensos de troca e pesos por paineis inferiores rolaveis.
- Manter troca preferida, equipamento indisponivel, pesos por exercicio e resumo de itens pulados.
- Android usa versionCode 1247 e versionName 12.4.7.

## v12.4.8 - Ciclo pessoal para a prova de 5 km

- Plano hibrido datado de 27/07/2026 a 16/08/2026.
- Treinos de musculacao na terca, quarta e sexta, com pernas sem corrida na quarta.
- Treze sessoes de corrida com dias uteis, prova exclusiva no domingo e nenhuma corrida no sabado.
- Pace central derivado de km/h, etapas completas e ajuste ativo de 0,1 km/h.
- Deload temporario na semana da prova e recuperacao de pernas para check-in vermelho.
- Preset aplicavel com confirmacao, IDs exclusivos e preservacao integral do historico e backup.
- Compatibilidade validada com backup v1, tolerancia a registros incompletos e desfazer com restauracao integral.
- Home e Running Coach passam a reconhecer inicio, semana real, prova e ciclo concluido.
- Android usa versionCode 1248 e versionName 12.4.8.

## v12.5.0 - Soundcore Sport X20, descoberta BLE ampliada

- Adicionar diagnostico debug com `BLUETOOTH_SCAN` somente apos autorizacao explicita.
- Procurar um possivel endpoint BLE separado do endereco CLASSIC/BR-EDR observado no aparelho real.
- Correlacionar resultados com estados fisicos dos fones e da caixa sem escrita GATT ou comandos proprietarios.
- Manter bateria esquerda, direita e caixa como `Indisponivel` enquanto nao houver evidencia tecnica reproduzivel.
- Integrar na Home apenas os campos confirmados por APIs publicas do Android.
