import os
import time
import hmac
import hashlib
from fastapi import HTTPException, status

def validate_timestamp(ts_header: str, tolerance: int):
    try:
        ts = int(ts_header)
    except Exception:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid timestamp")
    now = int(time.time())
    if abs(now - ts) > tolerance:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Timestamp out of range")
    return ts

def compute_hmac(secret: bytes, payload: bytes, timestamp: str) -> str:
    msg = timestamp.encode() + b"." + payload
    dig = hmac.new(secret, msg, hashlib.sha256).hexdigest()
    return dig

def verify_hmac_signature(signature: str, secret: bytes, payload: bytes, timestamp: str):
    expected = compute_hmac(secret, payload, timestamp)
    if not hmac.compare_digest(expected, signature):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid signature")