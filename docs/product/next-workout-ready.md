# Mo2 LOG v8.0.0 - Next Workout Ready

Objetivo desta entrega: abrir o app no treino e chegar rapido na sessao certa.

## Como usar no proximo treino

1. Suba o ambiente local com Docker.

```bash
docker compose up -d
```

2. Abra o frontend.

```text
http://localhost:5173
```

3. Entre com o botao de demo local.

4. Abra a tela Treino.

5. No card Proximo treino, clique em Carregar proximo treino.

6. Clique em Iniciar, registre as series e use o descanso automatico entre elas.

7. Se algum equipamento estiver ocupado, use Trocar exercicio no exercicio correspondente.

## Validacao rapida

- API: GET /api/v1/training-sessions/next-ready?user_id=1
- Release: GET /api/v1/product/release-notes deve retornar version 8.0.0
- Tela: /workout deve exibir o card Proximo treino antes da lista de exercicios

## Limites conhecidos

- A recomendacao busca sessoes planejadas ou em andamento nos proximos 14 dias.
- Corridas continuam sendo executadas pelo Running Coach; o card indica a sessao e orienta o aquecimento.
- O app Android nativo segue como trilha futura, com contrato Health Connect ja documentado nas entregas anteriores.
