from fastapi import FastAPI, UploadFile, File, Form, Depends, Header, HTTPException, status, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app import schemas
from app.services import uploads
from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from fastapi.responses import JSONResponse
from fastapi.requests import Request
from app.middleware.log_exceptions import log_exceptions
import os, time, hmac, hashlib, logging

limiter = Limiter(key_func=get_remote_address)
logging.basicConfig(level=logging.INFO)
app = FastAPI()
app.middleware("http")(log_exceptions)

@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request, exc):
    return JSONResponse(
        status_code=429,
        content={"detail": "Too Many Requests"}
    )

# ===== Bearer 토큰 검증 =====
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
security = HTTPBearer()
APP_BEARER_TOKEN = os.getenv("APP_BEARER_TOKEN", "")

def verify_bearer(credentials: HTTPAuthorizationCredentials = Depends(security)):
    if credentials.credentials != APP_BEARER_TOKEN:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid auth token")
    return credentials.credentials

# ===== HMAC 서명 검증 =====
HMAC_SECRET = os.getenv("HMAC_SECRET", "").encode()
HMAC_TOLERANCE = int(os.getenv("HMAC_TOLERANCE_SECONDS", "60"))

def verify_hmac(x_timestamp: str = Header(...), x_signature: str = Header(...), file_bytes: bytes = b""):
    try:
        ts = int(x_timestamp)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid timestamp")
    if abs(int(time.time()) - ts) > HMAC_TOLERANCE:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Timestamp out of range")

    msg = x_timestamp.encode() + b"." + file_bytes
    expected = hmac.new(HMAC_SECRET, msg, hashlib.sha256).hexdigest()
    if not hmac.compare_digest(expected, x_signature):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid signature")

# ===== 간단한 후처리 예시 =====
def post_process(upload_id: int):
    # 예: 로그 남기기, 알림 발송, 썸네일 생성 등
    print(f"[POST PROCESS] Upload {upload_id} 후처리 작업 실행")

# ===== 엔드포인트 =====
@app.post("/upload", response_model=schemas.UploadResponse, dependencies=[Depends(verify_bearer)])
@limiter.limit("5/second")
async def upload_file(
    request: Request,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    people_count: int = Form(...),
    db: AsyncSession = Depends(get_db),
    x_timestamp: str = Header(...),
    x_signature: str = Header(...),
    idempotency_key: str = Header(..., alias="Idempotency-Key")
):
    start_time = time.time()  # ⬅️ 처리 시간 측정 시작

    # 1. 파일 크기 제한 (10MB)
    file_size = 0
    file.file.seek(0, os.SEEK_END)
    file_size = file.file.tell()
    file.file.seek(0)
    if file_size > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large (max 10MB)")

    # 2. MIME 타입 검증
    if file.content_type not in ["image/jpeg", "image/png"]:
        raise HTTPException(status_code=415, detail="Unsupported file type")

    # 3. HMAC 검증
    content = await file.read()
    verify_hmac(x_timestamp, x_signature, content)
    file.file.seek(0)

    # 4. Idempotency-Key 중복 체크
    existing = await uploads.get_by_idempotency_key(db, idempotency_key)
    if existing:
        return schemas.UploadResponse(**dict(existing))

    # 5. 신규 업로드
    upload = await uploads.create_upload(
        db, file, file.filename, people_count, idempotency_key=idempotency_key
    )

    # 6. 후처리 작업 백그라운드 실행
    background_tasks.add_task(post_process, upload.id)

    elapsed = (time.time() - start_time) * 1000
    logging.info(
        f"UPLOAD | id={upload.id} | people_count={people_count} | "
        f"content_type={file.content_type} | elapsed={elapsed:.2f}ms"
    )

    return upload

@app.get("/uploads", response_model=list[schemas.UploadResponse])
async def get_uploads(
    skip: int = 0,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    rows = await uploads.list_uploads(db, skip=skip, limit=limit)
    return [schemas.UploadResponse(**dict(r)) for r in rows]