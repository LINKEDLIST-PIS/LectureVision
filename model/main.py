from fastapi import FastAPI, HTTPException
import threading
import uvicorn
import cv2
from .camera import capture_frame, current_camera_url, set_camera_url
from .detector import detect_people
from .processing import apply_mosaic
from .uploader import upload_image, ensure_token
from .monitor import CameraConfigDialog, start_monitoring, pause_monitor
import requests
from . import config

model = FastAPI(
    title="LectureVision Model Server",
    version="0.3.0"
)

def run_server():
    uvicorn.run("model.main:model", host="0.0.0.0", port=8000, reload=False)

def validate_ticket(ticket: str) -> dict | None:
    token = ensure_token()
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.post(
        f"{config.API_BASE}/tickets/validate",
        params={"ticket": ticket},
        headers=headers
    )
    if resp.status_code == 200:
        return resp.json()

    return None

@model.post("/measure")
def measure(ticket: str):
    global pause_monitor

    ticket_data = validate_ticket(ticket)
    if not ticket_data:
        raise HTTPException(status_code=403, detail="Invalid ticket")

    pause_monitor = True
    frame = capture_frame()
    people_count, boxes = detect_people(frame)
    mosaicked = apply_mosaic(frame, boxes)
    resp = upload_image(mosaicked, people_count, client_id=ticket_data["user_id"])
    pause_monitor = False

    try:
        api_response = resp.json()
    except ValueError:
        api_response = {"error": "Invalid response from upload API"}

    return {
        "people_count": people_count,
        "api_response": api_response
    }

if __name__ == "__main__":
    server_thread = threading.Thread(target=run_server, daemon=True)
    server_thread.start()
    start_monitoring()