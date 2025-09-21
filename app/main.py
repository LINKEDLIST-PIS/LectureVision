from fastapi import FastAPI, UploadFile, File, Form, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app import schemas
from app.services import uploads

app = FastAPI()

@app.post("/upload", response_model=schemas.UploadResponse)
async def upload_file(
    file: UploadFile = File(...),
    people_count: int = Form(...),
    db: AsyncSession = Depends(get_db)
):
    return await uploads.create_upload(db, file, file.filename, people_count)

@app.get("/uploads", response_model=list[schemas.UploadResponse])
async def get_uploads(
    skip: int = 0,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    rows = await uploads.list_uploads(db, skip=skip, limit=limit)
    return [schemas.UploadResponse(**dict(r)) for r in rows]