# Arquitetura Geral

O backend do Mo² LOG segue uma arquitetura em camadas:

```text
API Routes -> Services -> Repositories -> Database
```

A primeira fundação utiliza FastAPI, SQLAlchemy 2, Alembic e PostgreSQL.

A entidade central do domínio será `TrainingSession`, mas a versão atual inclui apenas a fundação técnica e o model inicial `User`.
