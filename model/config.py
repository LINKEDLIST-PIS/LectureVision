import os
from dotenv import load_dotenv

load_dotenv()

API_BASE = os.getenv("API_BASE")
API_TOKEN = os.getenv("API_TOKEN")
HMAC_SECRET = os.getenv("HMAC_SECRET")
CAMERA_RTSP = os.getenv("CAMERA_RTSP")

DEVICE = os.getenv("DEVICE", "cuda")
CONF_THRESHOLD = float(os.getenv("CONF_THRESHOLD", "0.25"))