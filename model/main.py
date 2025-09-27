import os
import time
import hmac
import hashlib
import uuid
import cv2
import requests
from fastapi import FastAPI
from ultralytics import YOLO
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

API_BASE = os.getenv("API_BASE")
API_TOKEN = os.getenv("API_TOKEN")
HMAC_SECRET = os.getenv("HMAC_SECRET")
CAMERA_RTSP = os.getenv("CAMERA_RTSP")
DEVICE = os.getenv("DEVICE", "cuda")
CONF_THRESHOLD = float(os.getenv("CONF_THRESHOLD", "0.25"))

# Load YOLO model
MODEL = YOLO("yolo11m.pt").to(DEVICE)

model = FastAPI(title="LectureVision Model Server", version="0.2.0")


def hmac_signature(ts, file_bytes):
    msg = ts.encode() + b"." + file_bytes
    return hmac.new(HMAC_SECRET.encode(), msg, hashlib.sha256).hexdigest()


def capture_frame():
    cap = cv2.VideoCapture(CAMERA_RTSP)
    ok, frame = cap.read()
    cap.release()
    if not ok:
        raise RuntimeError("카메라 프레임 캡처 실패")
    return frame


def detect_people(frame):
    results = MODEL.predict(frame, conf=CONF_THRESHOLD, verbose=False)
    count = 0
    boxes = []
    for r in results:
        for b in r.boxes:
            if r.names[int(b.cls)] == "person":
                x1, y1, x2, y2 = b.xyxy[0].tolist()
                boxes.append((x1, y1, x2, y2))
                count += 1
    return count, boxes


def apply_mosaic(bgr_frame, boxes, mosaic_ratio=0.08):
    h, w = bgr_frame.shape[:2]
    out = bgr_frame.copy()
    for (x1, y1, x2, y2) in boxes:
        x1 = max(0, int(x1)); y1 = max(0, int(y1))
        x2 = min(w, int(x2)); y2 = min(h, int(y2))
        roi = out[y1:y2, x1:x2]
        if roi.size == 0:
            continue
        small = cv2.resize(
            roi,
            (max(1, int((x2 - x1) * mosaic_ratio)), max(1, int((y2 - y1) * mosaic_ratio))),
            interpolation=cv2.INTER_LINEAR
        )
        mosaic = cv2.resize(small, (x2 - x1, y2 - y1), interpolation=cv2.INTER_NEAREST)
        out[y1:y2, x1:x2] = mosaic
    return out


def upload_image(file_bytes, people_count):
    ts = str(int(time.time()))
    sig = hmac_signature(ts, file_bytes)
    idem = f"req-{uuid.uuid4().hex[:8]}"

    headers = {
        "Authorization": f"Bearer {API_TOKEN}",
        "X-Timestamp": ts,
        "X-Signature": sig,
        "Idempotency-Key": idem,
    }
    files = {"file": ("frame.jpg", file_bytes, "image/jpeg")}
    data = {"people_count": str(people_count)}

    resp = requests.post(f"{API_BASE}/upload", headers=headers, files=files, data=data)
    return resp


@model.get("/health")
def health():
    return {"status": "ok"}


@model.post("/measure")
def measure():
    frame = capture_frame()
    people_count, boxes = detect_people(frame)
    mosaicked = apply_mosaic(frame, boxes)

    _, enc = cv2.imencode(".png", mosaicked)
    file_bytes = enc.tobytes()

    resp = upload_image(file_bytes, people_count)
    return {"people_count": people_count, "api_response": resp.json()}