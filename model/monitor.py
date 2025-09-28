import cv2
import threading
from .detector import detect_people
from .processing import apply_mosaic
from .config import CAMERA_RTSP

pause_monitor = False

def monitor_loop():
    cap = cv2.VideoCapture(CAMERA_RTSP)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    if not cap.isOpened():
        print("❌ Camera open failed")
        return

    while True:
        cap.grab()
        ret, frame = cap.read()
        if not ret:
            continue

        if pause_monitor:
            cv2.imshow("Model Server Monitor", frame)
            if cv2.waitKey(1) & 0xFF == 27:
                break
            continue

        people_count, boxes = detect_people(frame)
        mosaicked = apply_mosaic(frame, boxes)

        cv2.putText(mosaicked, f"Persons: {people_count}", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0,255,0), 2)

        cv2.imshow("Model Server Monitor", mosaicked)

        if cv2.waitKey(1) & 0xFF == 27:
            break

    cap.release()
    cv2.destroyAllWindows()

def start_monitoring():
    t = threading.Thread(target=monitor_loop, daemon=True)
    t.start()