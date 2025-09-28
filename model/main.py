from fastapi import FastAPI
import uvicorn

from .camera import capture_frame
from .detector import detect_people
from .processing import apply_mosaic
from .uploader import upload_image
from .monitor import start_monitoring

model = FastAPI(
    title="LectureVision Model Server",
    version="0.3.0"
)

@model.post("/measure")
def measure():
    global pause_monitor
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