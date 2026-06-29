# DER v1

## Tabela atual

```text
users
├── id PK
├── name
├── email UNIQUE
├── password_hash
├── avatar_url
├── created_at
└── updated_at
```

## Modelo planejado

```text
users
├── training_plans
│   └── training_sessions
│       ├── strength_workout_exercises
│       ├── running_activities
│       └── exercise_swap_logs
├── user_gym_equipment
├── goals
└── personal_records
```
