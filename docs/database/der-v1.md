# DER v1 — Mo² LOG

Entidade central futura: `training_sessions`.

Nesta release foram implementadas as entidades base de usuário e biblioteca de exercícios.

## Implementado até v0.4.0

```text
users

exercises
├── exercise_muscles
│   └── muscle_groups
├── exercise_equipment
│   └── equipment
└── exercise_alternatives

users
└── user_gym_equipment
    └── equipment
```

## Regra de equipamento indisponível

`user_gym_equipment.status = unavailable` remove exercícios dependentes daquele equipamento das sugestões padrão, mas eles continuam visíveis quando o usuário escolhe ver todas as opções.


## Analytics Core

```text
users 1 ─── N goals
users 1 ─── N personal_records
training_sessions 0..1 ─── N personal_records
running_activities 0..1 ─── N personal_records
```
