from fastapi import FastAPI, UploadFile, File, Form, Depends, Header, HTTPException, status, BackgroundTasks, APIRouter, Request
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app import schemas
from app.services import uploads
from app.security import AUDIENCE_SERVER, create_access_token, verify_bearer
from app.hmac_utils import verify_hmac_signature
from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse
from app.middleware.log_exceptions import log_exceptions
import os, time, logging

from app.accounts import models as account_models
from app.accounts.db import engine, Base
from app.accounts.routes import router as accounts_router

from app.tickets.routes import router as tickets_router

limiter = Limiter(key_func=get_remote_address)
logging.basicConfig(level=logging.INFO)

app = FastAPI()
router = APIRouter()
app.middleware("http")(log_exceptions)

Base.metadata.create_all(bind=engine)

@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request, exc):
    return JSONResponse(
        status_code=429,
        content={"detail": "Too Many Requests"}
    )

@router.post("/token")
def issue_token(model_server_id: str):
    access_token = create_access_token(
        {"sub": model_server_id},
        audience="server-client"
    )
    return {"access_token": access_token, "token_type": "bearer"}

@app.post("/upload", response_model=schemas.UploadResponse, dependencies=[Depends(verify_bearer)])
@limiter.limit("5/second")
async def upload_file(
    request: Request,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    people_count: int = Form(...),
    client_id: str = Form(...),
    db: AsyncSession = Depends(get_db),
    x_timestamp: str = Header(..., alias="X-Timestamp"),
    x_signature: str = Header(..., alias="X-Signature"),
    idempotency_key: str = Header(..., alias="Idempotency-Key"),
    payload: dict = Depends(verify_bearer)
):
    if payload.get("aud") != AUDIENCE_SERVER:
        raise HTTPException(status_code=403, detail="Only server tokens can upload")

    model_server_id = payload["sub"]
    start_time = time.time()

    file.file.seek(0, os.SEEK_END)
    file_size = file.file.tell()
    file.file.seek(0)
    if file_size > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large (max 10MB)")

    if file.content_type not in ["image/jpeg", "image/png"]:
        raise HTTPException(status_code=415, detail="Unsupported file type")

    content = await file.read()
    verify_hmac_signature(x_signature, os.getenv("HMAC_SECRET", "").encode(), content, x_timestamp)
    file.file.seek(0)

    existing = await uploads.get_by_idempotency_key(db, idempotency_key)
    if existing:
        return schemas.UploadResponse(**dict(existing))

    upload = await uploads.create_upload(
        db, file, file.filename, people_count,
        idempotency_key=idempotency_key, client_id=client_id
    )

    background_tasks.add_task(lambda: print(f"[POST PROCESS] Upload {upload.id}"))

    elapsed = (time.time() - start_time) * 1000
    logging.info(
        f"UPLOAD | id={upload.id} | people_count={people_count} | "
        f"content_type={file.content_type} | elapsed={elapsed:.2f}ms"
    )

    return upload

@app.get("/uploads", response_model=list[schemas.UploadResponse], dependencies=[Depends(verify_bearer)])
async def get_uploads(
    skip: int = 0,
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
    payload: dict = Depends(verify_bearer)
):
    client_id = payload["sub"]

    rows = await uploads.list_uploads(db, skip=skip, limit=limit, client_id=client_id)
    return [schemas.UploadResponse(**dict(r)) for r in rows]

app.include_router(router)
app.include_router(accounts_router)
app.include_router(tickets_router)
