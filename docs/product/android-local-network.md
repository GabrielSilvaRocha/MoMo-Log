# Usar o Mo2 LOG no Android pela rede local

Release: v8.0.1 - Android Local Network Ready.

Este modo abre o app no navegador do smartphone usando o Docker rodando no PC.

## Requisitos

- PC e Android conectados na mesma rede Wi-Fi.
- Docker rodando no PC.
- Portas 5173 e 8000 liberadas no firewall do Windows para rede privada.

## Passo a passo

1. No PC, descubra o IPv4 da rede Wi-Fi.

```powershell
ipconfig
```

Procure algo como `Endereco IPv4 . . . : 192.168.0.25`.

2. Suba o app no PC.

```bash
docker compose up -d --build frontend backend
```

3. No Android, abra o Chrome e acesse usando o IP do PC.

```text
http://SEU_IP_DO_PC:5173
```

Exemplo:

```text
http://192.168.0.25:5173
```

4. Entre com Demo Local, abra Treino e toque em Carregar proximo treino.

## Teste de diagnostico

No Android, abra:

```text
http://SEU_IP_DO_PC:8000/api/v1/health
```

Se a API responder, backend e rede estao acessiveis. Se o frontend abrir mas as chamadas falharem, confira firewall e se o celular esta na mesma rede do PC.

## Observacoes

- Nao use `localhost` no Android; no celular ele aponta para o proprio Android.
- O app ainda nao e um APK nativo. Esta etapa usa o navegador do Android como ponte pratica para treinar hoje.
- Para acesso fora da sua casa/academia, o caminho correto e publicar em cloud com HTTPS.
