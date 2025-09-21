from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.exc import IntegrityError
from app import models
from app.services.storage import save_file

async def get_by_idempotency_key(db: AsyncSession, key: str):
    """Idempotency-Key로 업로드 조회"""
    result = await db.execute(
        select(models.Upload).where(models.Upload.idempotency_key == key)
    )
    row = result.first()
    return row[0] if row else None

async def create_upload(
    db: AsyncSession,
    file,
    original_name: str,
    people_count: int,
    idempotency_key: str = None
):
    stored_name, abs_path = save_file(file, original_name)
    upload = models.Upload(
        original_name=original_name,
        stored_name=stored_name,
        abs_path=abs_path,
        people_count=people_count,
        idempotency_key=idempotency_key
    )
    db.add(upload)
    try:
        await db.commit()
    except IntegrityError:
        # UNIQUE 제약 위반 → 기존 데이터 반환
        await db.rollback()
        existing = await get_by_idempotency_key(db, idempotency_key)
        return existing
    await db.refresh(upload)
    return upload

async def list_uploads(db: AsyncSession, skip: int = 0, limit: int = 50):
    result = await db.execute(
        models.Upload.__table__.select().offset(skip).limit(limit)
    )
    return result.fetchall()