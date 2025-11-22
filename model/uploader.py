import time
import hmac
import hashlib
import uuid
import cv2
import requests
import jwt, time
from . import config

def get_token():
    resp = requests.post(
        f"{config.API_BASE}/token",
        params={"model_server_id": config.MODEL_SERVER_ID}
    )
    resp.raise_for_status()
    data = resp.json()
    config.API_TOKEN = data["access_token"]
    return config.API_TOKEN

def decode_token(token: str, secret: str = None):
    payload = jwt.decode(token, options={"verify_signature": False})
    return payload

def ensure_token():
    if not config.API_TOKEN:
        return get_token()
    payload = decode_token(config.API_TOKEN)
    exp = payload["exp"]
    if time.time() > exp:
        return get_token()
    return config.API_TOKEN

def upload_image(frame, people_count: int):
    success, enc = cv2.imencode(".png", frame)
    if not success:
        raise RuntimeError("Image encoding failed")
    file_bytes = enc.tobytes()

    timestamp = str(int(time.time()))

    secret = config.HMAC_SECRET.encode() if isinstance(config.HMAC_SECRET, str) else config.HMAC_SECRET
    message = timestamp.encode() + b"." + file_bytes
    signature = hmac.new(secret, message, hashlib.sha256).hexdigest()

    idempotency_key = f"model-req-{uuid.uuid4()}"

    token = ensure_token()

    headers = {
        "Authorization": f"Bearer {token}",
        "X-Timestamp": timestamp,
        "X-Signature": signature,
        "Idempotency-Key": idempotency_key,
    }

    files = {
        "file": ("frame.png", file_bytes, "image/png")
    }
    data = {
        "people_count": people_count, 
        "client_id": client_id
    }

    resp = requests.post(f"{config.API_BASE}/upload", headers=headers, files=files, data=data)
    resp.raise_for_status()
    return resp