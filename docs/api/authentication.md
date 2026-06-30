# Autenticação

A autenticação do Mo² LOG usa token assinado no backend. Nesta fase, a implementação usa HS256 com biblioteca padrão do Python para manter a fundação simples e sem dependências adicionais.

## Endpoints

### Cadastrar usuário

```http
POST /api/v1/auth/register
```

```json
{
  "name": "Gabriel Rocha",
  "email": "gabriel@example.com",
  "password": "senha-segura"
}
```

### Login

```http
POST /api/v1/auth/login
```

```json
{
  "email": "gabriel.demo@mo2log.com.br",
  "password": "<sua-senha>"
}
```

### Login demo

```http
POST /api/v1/auth/demo-login
```

### Usuário atual

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

## Preferências do usuário

A tabela `user_preferences` guarda preferências como:

- fonte padrão de corrida;
- dias preferenciais de treino;
- meta semanal de corrida;
- meta semanal de musculação;
- observações da academia.
