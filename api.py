# utils/api.py
import requests

SERVER_URL = 'https://your.api.server/measure'

def measure_via_api(filepath):
    try:
        with open(filepath, 'rb') as f:
            files = {'image': ('img.jpg', f, 'image/jpeg')}
            resp = requests.post(SERVER_URL, files=files, timeout=15)
            if resp.status_code == 200:
                return resp.json()
    except Exception as e:
        print('measure error', e)
    return None
