from fastapi import FastAPI, HTTPException
import uvicorn

from .camera import capture_frame
from .detector import detect_people
from .processing import apply_mosaic
from .uploader import upload_image, ensure_token
from .monitor import start_monitoring, pause_monitor
import requests
from . import config

model = FastAPI(
    title="LectureVision Model Server",
    version="0.3.0"
)

def validate_ticket(ticket: str) -> bool:
    token = ensure_token()
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.post(
        f"{config.API_BASE}/tickets/validate",
        params={"ticket": ticket},
        headers=headers
    )
    return resp.status_code == 200

@model.post("/measure")
def measure(ticket: str):
    global pause_monitor

    if not validate_ticket(ticket):
        raise HTTPException(status_code=403, detail="Invalid ticket")
    pause_monitor = True
    frame = capture_frame()
    people_count, boxes = detect_people(frame)
    mosaicked = apply_mosaic(frame, boxes)
    resp = upload_image(mosaicked, people_count)
    pause_monitor = False

    return {
        "people_count": people_count,
        "api_response": resp.json()
    }

if __name__ == "__main__":
    start_monitoring()
    uvicorn.run("model.main:model", host="0.0.0.0", port=8000, reload=True)