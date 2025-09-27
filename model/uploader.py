import time
import hmac
import hashlib
import uuid
import cv2
import requests
from .config import API_BASE, API_TOKEN, HMAC_SECRET

def upload_image(frame, people_count: int):
    success, enc = cv2.imencode(".png", frame)
    if not success:
        raise RuntimeError("Image encoding failed")
    file_bytes = enc.tobytes()

    timestamp = str(int(time.time()))

    secret = HMAC_SECRET.encode() if isinstance(HMAC_SECRET, str) else HMAC_SECRET
    message = timestamp.encode() + b"." + file_bytes
    signature = hmac.new(secret, message, hashlib.sha256).hexdigest()

    idempotency_key = f"model-req-{uuid.uuid4()}"

    headers = {
        "Authorization": f"Bearer {API_TOKEN}",
        "X-Timestamp": timestamp,
        "X-Signature": signature,
        "Idempotency-Key": idempotency_key,
    }

    files = {
        "file": ("frame.png", file_bytes, "image/png")
    }
    data = {
        "people_count": people_count
    }

    resp = requests.post(f"{API_BASE}/upload", headers=headers, files=files, data=data)

    return resp