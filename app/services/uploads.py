from sqlalchemy.ext.asyncio import AsyncSession
from app import models, schemas
from app.services.storage import save_file

async def create_upload(db: AsyncSession, file, original_name: str, people_count: int):
    stored_name, abs_path = save_file(file, original_name)
    upload = models.Upload(
        original_name=original_name,
        stored_name=stored_name,
        abs_path=abs_path,
        people_count=people_count
    )
    db.add(upload)
    await db.commit()
    await db.refresh(upload)
    return upload

async def list_uploads(db: AsyncSession, skip: int = 0, limit: int = 50):
    result = await db.execute(
        models.Upload.__table__.select().offset(skip).limit(limit)
    )
    return result.fetchall()
