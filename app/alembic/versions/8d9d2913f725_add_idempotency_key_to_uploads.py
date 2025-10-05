"""add idempotency_key to uploads

Revision ID: 8d9d2913f725
Revises: 4eb2c4e283e3
Create Date: 2025-09-21 11:54:51.823848

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '8d9d2913f725'
down_revision: Union[str, Sequence[str], None] = '4eb2c4e283e3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # uploads 테이블에 idempotency_key 컬럼 추가
    op.add_column(
        'uploads',
        sa.Column('idempotency_key', sa.String(length=64), nullable=True)
    )
    # UNIQUE 제약 조건 추가
    op.create_unique_constraint(
        'uq_uploads_idempotency_key',  # 제약 이름
        'uploads',                     # 테이블 이름
        ['idempotency_key']             # 컬럼 목록
    )

def downgrade() -> None:
    """Downgrade schema."""
    # UNIQUE 제약 조건 제거
    op.drop_constraint(
        'uq_uploads_idempotency_key',
        'uploads',
        type_='unique'
    )
    # 컬럼 제거
    op.drop_column('uploads', 'idempotency_key')
