import os
from dotenv import load_dotenv

load_dotenv()

MODEL_SERVER_ID = "YOLOv11M"
API_BASE = os.getenv("API_BASE")
API_TOKEN = None
HMAC_SECRET = os.getenv("HMAC_SECRET")
CAMERA_RTSP = os.getenv("CAMERA_RTSP")

DEVICE = os.getenv("DEVICE", "cuda")
CONF_THRESHOLD = float(os.getenv("CONF_THRESHOLD", "0.25"))