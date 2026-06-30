# Integrações

## Corridas sem integrações externas

A partir da v6.1.0, o Mo² LOG não depende de Strava, Health Connect ou outras APIs externas para o fluxo principal de corrida.

O módulo de corrida passa a ser o **Running Coach**:

1. Usuário define objetivo de corrida, inicialmente prova de 5 km.
2. Backend gera plano progressivo até a data da prova.
3. Frontend guia a execução na esteira com pace, velocidade e cronômetro regressivo.
4. Treinos intervalados são divididos em etapas, incluindo recuperação/descanso.
5. Botões `+` e `-` ajustam a velocidade da esteira em 0,1 km/h.
6. Cada ajuste é registrado para análise futura.

## Fontes mantidas

- `running_coach`: plano e execução guiada.
- `manual_treadmill`: registro manual de corrida na esteira.
- `manual_outdoor`: registro manual externo, se necessário.

Integrações externas podem voltar no futuro, mas não fazem parte do MVP principal.
