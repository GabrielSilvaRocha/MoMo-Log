"""clear demo password hash

Revision ID: 20260630_0007
Revises: 20260630_0006
Create Date: 2026-06-30
"""
from typing import Sequence, Union

from alembic import op

revision: str = "20260630_0007"
down_revision: Union[str, None] = "20260630_0006"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        """
        UPDATE users
        SET password_hash = NULL
        WHERE id = 1 AND email = 'gabriel.demo@mo2log.com.br';
        """
    )


def downgrade() -> None:
    # Do not restore repository-defined demo credentials.
    pass
