# Descoberta de bateria - Soundcore Sport X20

Este documento registra somente evidencias obtidas no aparelho real. O APK debug coleta metadados publicos e faz leituras sequenciais apenas de caracteristicas GATT com `PROPERTY_READ`. Ele nao escreve, ativa notificacoes, altera MTU ou prioridade, faz scan, pareia ou envia comandos ao fone.

Copie o relatorio pelo botao `Copiar diagnostico` e cole os trechos relevantes nas tabelas abaixo. Enderecos Bluetooth devem permanecer mascarados no formato `AA:BB:**:**:EE:FF`.

## Ambiente de teste

**Status:** VALIDACAO FISICA PARCIAL EM 2026-07-21

Data e horario: 2026-07-21 23:52:59 (inicio informado pela ferramenta)

Responsavel: validacao assistida pelo Codex via ADB; manipulacao dos fones ainda pendente

Observacoes do ambiente: APK debug instalado com `adb install -r`; Activity iniciou no celular real e uma tentativa GATT encerrou por timeout global de oito segundos sem travamento.

## Versoes

- Mo2 Log: 12.3.1 (debug)
- Android: 16 / API 36
- Modelo do celular: Xiaomi 2311DRK48G
- Firmware do Sport X20: AGUARDANDO TESTE FISICO
- Versao do app Soundcore: AGUARDANDO TESTE FISICO

## Identificacao Bluetooth

**Status:** VALIDACAO FISICA PARCIAL EM 2026-07-21

| Campo | Resultado |
| --- | --- |
| Nome | soundcore Sport X20 |
| Alias | soundcore Sport X20 |
| Endereco mascarado | `7C:E9:**:**:E1:52` |
| Tipo | CLASSIC |
| Classe | `device=0x404 major=0x400` |
| Pareamento | BONDED |
| Transporte | BR/EDR; a inspecao tentou LE |
| Quantidade de candidatos | 1 |
| Origem da selecao | SAVED_ADDRESS |
| Endpoint BLE separado observado | AGUARDANDO TESTE FISICO |

## Estado dos perfis

**Status:** VALIDACAO FISICA PARCIAL EM 2026-07-21

| Perfil/estado | Resultado |
| --- | --- |
| ACL | CONNECTED, inferido pelos perfis publicos |
| A2DP | CONNECTED |
| Headset | CONNECTED |

## Servicos GATT encontrados

**Status:** AGUARDANDO TESTE FISICO

A tentativa observada encerrou por timeout global de oito segundos. Isso nao confirma ausencia de servicos; nenhuma lista de UUIDs foi classificada como evidencia nesta validacao parcial.

| UUID | Propriedades | Valor bruto | Cenario | Interpretacao | Evidencia |
| --- | --- | --- | --- | --- | --- |
| AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO |

## Caracteristicas encontradas

**Status:** AGUARDANDO TESTE FISICO

| UUID | Propriedades | Valor bruto | Cenario | Interpretacao | Evidencia |
| --- | --- | --- | --- | --- | --- |
| AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO |

UUIDs desconhecidos devem permanecer com interpretacao `DESCONHECIDA`. Um payload de um byte, isoladamente, nao confirma bateria.

## Battery Service padrao

**Status:** AGUARDANDO TESTE FISICO

| Campo | Resultado |
| --- | --- |
| Service `0000180f-0000-1000-8000-00805f9b34fb` presente | AGUARDANDO TESTE FISICO |
| Characteristic `00002a19-0000-1000-8000-00805f9b34fb` presente | AGUARDANDO TESTE FISICO |
| Propriedades da characteristic | AGUARDANDO TESTE FISICO |
| Resultado da leitura | AGUARDANDO TESTE FISICO |
| Valor no Android | AGUARDANDO TESTE FISICO |
| Valor no app Soundcore | AGUARDANDO TESTE FISICO |

## Comparacao com o aplicativo Soundcore

**Status:** AGUARDANDO TESTE FISICO

| Cenario | Esquerdo | Direito | Caixa | Valor padrao observado | Observacoes |
| --- | --- | --- | --- | --- | --- |
| AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO | AGUARDANDO TESTE FISICO |

## Cenario A - Estado inicial

**Status:** VALIDACAO FISICA PARCIAL; ETAPAS RESTANTES AGUARDANDO TESTE FISICO

Fones conectados ao Android; A2DP, Headset e ACL apareceram conectados. A posicao fisica dos fones, ausencia de reproducao e valores do app Soundcore nao foram comprovados pelo acesso ADB.

Resultado: identificacao e perfis confirmados; inspecao GATT terminou por timeout de oito segundos; servicos, caracteristicas e baterias continuam AGUARDANDO TESTE FISICO.

## Cenario B - Audio ativo

**Status:** AGUARDANDO TESTE FISICO

Reproduzir musica por ao menos dois minutos e atualizar a leitura. Confirmar se houve pausa, mudanca de volume, desconexao ou falha perceptivel.

Resultado:

## Cenario C - Niveis diferentes

**Status:** AGUARDANDO TESTE FISICO

Executar apenas quando esquerdo e direito mostrarem niveis diferentes no app Soundcore. Comparar campos que variem separadamente sem concluir significado a partir de um unico teste.

Resultado:

## Cenario D - Somente esquerdo

**Status:** AGUARDANDO TESTE FISICO

Manter o esquerdo fora e o direito dentro da caixa; atualizar e registrar todas as diferencas.

Resultado:

## Cenario E - Somente direito

**Status:** AGUARDANDO TESTE FISICO

Manter o direito fora e o esquerdo dentro da caixa; atualizar e registrar todas as diferencas.

Resultado:

## Cenario F - Caixa aberta

**Status:** AGUARDANDO TESTE FISICO

Colocar ambos os fones na caixa, abrir a tampa e atualizar a leitura.

Resultado:

## Cenario G - Caixa fechada

**Status:** AGUARDANDO TESTE FISICO

Fechar a caixa com ambos os fones, aguardar o comportamento normal e registrar se o dispositivo continua acessivel.

Resultado:

## Cenario H - Caixa carregando

**Status:** AGUARDANDO TESTE FISICO

Conectar a caixa ao carregador, abrir a tampa e comparar o diagnostico com LEDs e app Soundcore.

Resultado:

## Cenario I - Reconexao

**Status:** AGUARDANDO TESTE FISICO

Desligar o Bluetooth, confirmar `BLUETOOTH_DISABLED`, ligar novamente, reconectar e solicitar nova leitura.

Resultado:

## Cenario J - Permissao

**Status:** AGUARDANDO TESTE FISICO

Revogar `BLUETOOTH_CONNECT`, confirmar `PERMISSION_REQUIRED`, conceder novamente e verificar recuperacao sem reinstalar.

Resultado:

## Campos confirmados

AGUARDANDO TESTE FISICO

Nenhum campo de bateria separado esta confirmado neste momento. O unico formato interpretado automaticamente e o Battery Level oficial `0x2A19`, quando presente e valido.

## Campos ainda desconhecidos

- Bateria esquerda: AGUARDANDO TESTE FISICO
- Bateria direita: AGUARDANDO TESTE FISICO
- Bateria da caixa: AGUARDANDO TESTE FISICO
- Faixa de bateria da caixa: AGUARDANDO TESTE FISICO
- UUIDs proprietarios: AGUARDANDO TESTE FISICO
- Endpoint BLE separado: AGUARDANDO TESTE FISICO

## Riscos observados

**Resultados reais:** uma tentativa GATT expirou de forma recuperavel, restaurou a tela e nao derrubou o aplicativo. Impacto sobre audio, volume e consumo continua AGUARDANDO TESTE FISICO.

Riscos previstos: o firmware pode recusar GATT no endpoint de audio, encerrar a conexao, despertar o fone ou competir temporariamente com o app Soundcore. A ferramenta limita cada tentativa a oito segundos e sempre solicita `disconnect()` e `close()` ao concluir, cancelar, falhar ou expirar.

## Conclusao

Validacao parcial confirma identificacao do Sport X20 pareado e estados A2DP/Headset conectados no aparelho Xiaomi. A tentativa LE observada expirou antes de produzir evidencia classificavel de servicos ou bateria.

Nao ha evidencia suficiente para implementar bateria esquerda, direita, caixa ou protocolo proprietario.

## Proximos experimentos seguros

1. Executar os cenarios A-J e anexar o relatorio copiado em cada estado.
2. Comparar UUIDs e payloads entre pelo menos dois niveis reais de bateria.
3. Verificar se existe endpoint BLE separado sem adicionar permissao de scan nesta etapa.
4. Parar e solicitar autorizacao antes de qualquer `BLUETOOTH_SCAN` ou parser proprietario.
5. Nunca testar escrita, descritores, notificacoes desconhecidas ou comandos de controle.

## Modelo de relatorio copiado

```text
MO2 LOG - DIAGNOSTICO SOUNDCORE SPORT X20
Gerado em: <timestamp>

[AMBIENTE]
<dados coletados pelo APK debug>

[DISPOSITIVO]
Endereco: AA:BB:**:**:EE:FF

[ESTADO DO REPOSITORY]
<valores ausentes aparecem como Indisponivel>

[SERVICOS GATT]
<UUIDs e propriedades encontrados>

[LEITURAS]
<status, hexadecimal, decimal e interpretacao>

[EVENTOS]
<callbacks em ordem de timestamp>
```
