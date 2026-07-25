# Soundcore Sport X20 - infraestrutura de bateria

## Objetivo

Esta fase prepara o aplicativo Android nativo para identificar um Anker Soundcore Sport X20 ja pareado, acompanhar o estado da conexao e tentar uma leitura segura de bateria por APIs publicas do Android.

A implementacao e local, nao usa backend, internet, scan Bluetooth ou protocolo proprietario. A Home ainda nao exibe essas informacoes.

## Permissoes

- Android 8 a 11: `BLUETOOTH`, limitada no manifest a `maxSdkVersion=30`.
- Android 12 ou superior: `BLUETOOTH_CONNECT`, concedida em runtime.
- Bluetooth e declarado como recurso opcional.
- Nao sao solicitadas `BLUETOOTH_SCAN`, localizacao ou `BLUETOOTH_ADMIN`.

O dialogo de permissao nao e aberto ao iniciar o app. Enquanto nao existir uma acao visual dedicada, a permissao pode ser concedida nas configuracoes do Android ou, em um aparelho de desenvolvimento, com:

```powershell
adb shell pm grant br.com.mo2log.mobile android.permission.BLUETOOTH_CONNECT
```

Se a permissao for recusada, o monitor publica `PERMISSION_REQUIRED`, nao tenta acessar os pareados e o restante do Mo2 LOG continua funcionando.

## Identificacao do dispositivo

O monitor consulta somente dispositivos pareados. Ele procura primeiro o endereco salvo e depois nomes normalizados que contenham `soundcore sport x20` ou `sport x20`.

Mais de um candidato produz `MULTIPLE_CANDIDATES`. O app nao escolhe o primeiro arbitrariamente, nao inicia pareamento, nao faz scan e nao altera nome ou alias.

## Conexao

O estado e atualizado por broadcasts publicos de Bluetooth, ACL, A2DP e Headset. Proxies de perfil sao usados somente para consulta e sempre fechados. O app nunca conecta ou desconecta o perfil de audio, solicita foco de audio ou controla reproducao.

O monitor pode representar:

- Bluetooth ausente ou desligado;
- permissao necessaria;
- fone nao pareado ou selecao ambigua;
- pareado desconectado ou conectando;
- conectado com bateria;
- conectado sem dados de bateria;
- erro inesperado recuperavel.

## Leitura padrao de bateria

A unica leitura implementada usa os identificadores padronizados:

```text
Battery Service: 0000180f-0000-1000-8000-00805f9b34fb
Battery Level:   00002a19-0000-1000-8000-00805f9b34fb
```

A sessao GATT e assincrona, somente leitura e tem timeout de 8 segundos. Nao ha escrita, polling, alteracao de MTU, prioridade ou notificacoes. Toda sessao termina com tentativa de `disconnect()` e `close()`.

Servico ou caracteristica ausente significam capacidade indisponivel, nao erro fatal. Payload deve conter exatamente um byte entre 0 e 100.

## Valores atualmente possiveis

- `combinedBatteryPercent`: disponivel somente se o endpoint expuser o Battery Service padrao.
- `leftBatteryPercent`: indisponivel nesta fase.
- `rightBatteryPercent`: indisponivel nesta fase.
- `caseBatteryPercent`: indisponivel nesta fase.
- `caseBatteryRange`: indisponivel nesta fase.

Uma leitura combinada nunca e duplicada nos campos esquerdo e direito. Faixas da caixa nunca sao convertidas em porcentagem exata.

`SPORT_X20_PROPRIETARY_PROTOCOL` existe apenas como extensao futura. Nenhum dado desta fase recebe essa origem e nenhum comando proprietario e enviado.

## Modo diagnostico debug

Builds `debug` exibem `Diagnostico Soundcore` em `Mais`. A ferramenta fica em uma Activity separada, enumera servicos, caracteristicas e descritores e le sequencialmente apenas caracteristicas com `PROPERTY_READ`. O botao `Atualizar leitura` inicia uma unica sessao com timeout de oito segundos; uma tentativa anterior e cancelada antes da nova.

`Limpar diagnostico` remove somente os eventos temporarios. `Copiar diagnostico` gera texto simples com enderecos Bluetooth mascarados. UUID desconhecido e sempre rotulado `DESCONHECIDA` e nunca preenche o estado de bateria.

A Activity e seu manifesto existem apenas em `src/debug`. A entrada de `Mais` tambem depende de `BuildConfig.DEBUG`, portanto builds `release` nao mostram nem registram a ferramenta.

O roteiro e o documento de evidencias ficam em [`soundcore-sport-x20-battery-discovery.md`](soundcore-sport-x20-battery-discovery.md). Todos os resultados fisicos permanecem pendentes ate observacao no celular e no Sport X20 reais.

## Persistencia e privacidade

O arquivo existente `mo2log_native` guarda somente o dispositivo escolhido, ultima bateria valida, horario e origem. Objetos Bluetooth, servicos, caracteristicas e payloads brutos nao sao persistidos.

Ao restaurar o aplicativo, a ultima leitura e marcada como desatualizada. Mudancas de conexao nao alteram falsamente o horario da bateria. Estados temporarios sem bateria nao sobrescrevem uma leitura valida.

O endereco completo nunca aparece em mensagens de erro ou no log resumido. O endereco pareado e armazenado localmente apenas para reencontrar o dispositivo correto.

## Riscos e mitigacoes

Consultar pareados e perfis e uma operacao somente leitura. A tentativa GATT pode despertar o fone, consumir energia ou competir temporariamente com o aplicativo Soundcore em alguns firmwares.

Por isso, a leitura ocorre uma vez ao iniciar a observacao ou por atualizacao manual, nunca em polling. Ela e cancelada quando a Activity para ou e destruida. Nenhuma operacao altera o canal de audio.

## Validacao em aparelho fisico

1. Pareie o Sport X20 nas configuracoes do Android.
2. Conecte os fones e confirme que o audio funciona normalmente.
3. Instale o APK debug com `adb install -r`.
4. Conceda `Dispositivos proximos` nas configuracoes ou use o comando `pm grant` acima.
5. Inicie o Mo2 LOG.
6. Observe `adb logcat -s Mo2SportX20:D`.
7. Confirme `CONNECTED` ou `CONNECTED_WITHOUT_BATTERY_DATA` sem travamento.
8. Se houver bateria combinada, compare o valor com Android e aplicativo Soundcore.
9. Retire apenas um fone, recoloque no estojo e abra/feche a caixa; nenhum campo separado deve ser inventado.
10. Reproduza musica durante o teste e confirme que nao houve pausa, troca de volume ou desconexao causada pelo Mo2 LOG.
11. Desligue e ligue o Bluetooth e confirme as transicoes de estado.
12. Revogue a permissao e confirme que o restante do app continua utilizavel.

O resultado depende do firmware e dos endpoints expostos pelo Sport X20. A leitura da caixa, lado esquerdo e lado direito nao deve ser considerada suportada antes de existir evidencia tecnica em aparelho real.

## Desativar ou remover

Para desativar sem alterar o app, revogue a permissao `Dispositivos proximos`. O monitor permanecera inativo e o restante do aplicativo continuara normal.

Para remover a infraestrutura do codigo, retire o pacote `br.com.mo2log.mobile.bluetooth`, a integracao de ciclo de vida da `MainActivity` e as declaracoes Bluetooth do manifest. Nenhuma tela ou dado de treino depende dessas classes.
