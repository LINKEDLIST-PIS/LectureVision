import cv2
from .config import CAMERA_RTSP

current_camera_url = CAMERA_RTSP

def set_camera_url(url: str):
    global current_camera_url
    current_camera_url = url

def capture_frame():
    cap = cv2.VideoCapture(current_camera_url)
    ret, frame = cap.read()
    cap.release()
    if not ret:
        raise RuntimeError("Camera read failed")
    return frame