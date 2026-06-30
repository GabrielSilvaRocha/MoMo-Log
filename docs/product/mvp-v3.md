# v3.0.0 — Usuários e Preferências

Esta release introduz a camada de usuário real no Mo² LOG.

## Fluxos entregues

- Login demo.
- Cadastro de novo usuário.
- Login com e-mail e senha.
- Recuperação da sessão local no frontend.
- Perfil do usuário.
- Preferências persistentes.
- Fonte padrão de corrida para priorizar esteira manual.

## Decisão de produto

O Mo² LOG deixa de depender do Strava como fonte principal. A fonte padrão passa a ser configurável por usuário, com `manual_treadmill` como opção recomendada para o cenário atual.
