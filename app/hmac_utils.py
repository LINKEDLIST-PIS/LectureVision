import os
import time
import hmac
import hashlib
from fastapi import HTTPException, status

HMAC_TOLERANCE_SECONDS = int(os.getenv("HMAC_TOLERANCE_SECONDS", "60"))

def validate_timestamp(ts_header: str) -> int:
    try:
        ts = int(ts_header)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid timestamp"
        )
    now = int(time.time())
    if abs(now - ts) > HMAC_TOLERANCE_SECONDS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Timestamp out of range"
        )
    return ts

def compute_hmac(secret: bytes, payload: bytes, timestamp: str) -> str:
    msg = timestamp.encode() + b"." + payload
    return hmac.new(secret, msg, hashlib.sha256).hexdigest()

def verify_hmac_signature(signature: str, secret: bytes, payload: bytes, timestamp: str):
    validate_timestamp(timestamp)
    expected = compute_hmac(secret, payload, timestamp)
    if not hmac.compare_digest(expected, signature):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid signature"
        )
