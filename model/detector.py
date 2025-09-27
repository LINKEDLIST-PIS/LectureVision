import torch
from ultralytics import YOLO
from .config import DEVICE, CONF_THRESHOLD

MODEL = YOLO("yolo11m.pt").to(DEVICE)

def detect_people(frame):
    results = model(frame, conf=CONF_THRESHOLD, verbose=False)
    boxes = []
    for b in results[0].boxes:
        if int(b.cls) == 0: 
            x1, y1, x2, y2 = map(int, b.xyxy[0].tolist())
            boxes.append((x1, y1, x2, y2))
    return len(boxes), boxes