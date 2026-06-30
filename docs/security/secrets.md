# Política de secrets

O Mo² LOG não deve versionar senhas, tokens, secrets, client secrets ou arquivos `.env` reais.

## Permitido

- `.env.example`
- `.env.production.example`
- placeholders sem valor real
- documentação sem senha real

## Bloqueado

- `backend/.env`
- tokens OAuth
- client secrets de integrações externas futuras
- senhas demo
- chaves JWT reais

## Validação local

```bash
git grep -n "<senha-demo-antiga>"
git grep -n "CLIENT_SECRET"
git ls-files | grep -E '(^|/)\.env$'
```

Se `backend/.env` aparecer versionado:

```bash
git rm --cached backend/.env
```
