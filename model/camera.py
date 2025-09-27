import cv2
from .config import CAMERA_RTSP

def capture_frame():
    cap = cv2.VideoCapture(CAMERA_RTSP)
    ret, frame = cap.read()
    cap.release()
    if not ret:
        raise RuntimeError("Camera read failed")
    return frame