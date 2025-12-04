import cv2
import threading
import pynvml
from .detector import detect_people
from .processing import apply_mosaic
from .camera import current_camera_url, set_camera_url
from PyQt6.QtWidgets import QApplication, QMainWindow, QLabel, QDialog, QVBoxLayout, QLineEdit, QPushButton, QToolBar
from PyQt6.QtCore import QTimer
from PyQt6.QtGui import QImage, QPixmap, QGuiApplication, QAction
import sys

pynvml.nvmlInit()
handle = pynvml.nvmlDeviceGetHandleByIndex(0)
pause_monitor = False

def get_gpu_usage():
    util = pynvml.nvmlDeviceGetUtilizationRates(handle)
    return util.gpu

class MonitorWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Model Server Monitor")

        self.video_label = QLabel()
        self.video_label.setScaledContents(True)
        self.setCentralWidget(self.video_label)

        toolbar = QToolBar("Main Toolbar")
        self.addToolBar(toolbar)

        config_action = QAction("카메라 설정", self)
        config_action.triggered.connect(self.open_camera_config)
        toolbar.addAction(config_action)

        self.cap = cv2.VideoCapture(current_camera_url)
        self.cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

        self.timer = QTimer()
        self.timer.timeout.connect(self.update_frame)
        self.timer.start(30)

        screen = QGuiApplication.primaryScreen()
        size = screen.availableGeometry()
        self.setMinimumSize(800, 600)
        self.setMaximumSize(size.width(), size.height())
        self.showMaximized()

    def update_frame(self):
        global pause_monitor
        if pause_monitor:
            return

        for _ in range(5):
            self.cap.grab()
        ret, frame = self.cap.retrieve()
        if not ret:
            return

        people_count, boxes = detect_people(frame)
        mosaicked = apply_mosaic(frame, boxes)
        gpu_usage = get_gpu_usage()

        cv2.putText(mosaicked, f"GPU: {gpu_usage}%", (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0,255,0), 2)

        cv2.putText(mosaicked, f"Persons: {people_count}", (10, 70),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0,255,0), 2)

        rgb = cv2.cvtColor(mosaicked, cv2.COLOR_BGR2RGB)
        h, w, ch = rgb.shape
        qimg = QImage(rgb.data, w, h, ch*w, QImage.Format.Format_RGB888)
        self.video_label.setPixmap(QPixmap.fromImage(qimg))

    def open_camera_config(self):
        dialog = CameraConfigDialog(self)
        if dialog.exec():
            new_url = dialog.get_url()
            set_camera_url(new_url)
            self.cap.release()
            self.cap = cv2.VideoCapture(new_url)

class CameraConfigDialog(QDialog):
    def get_url(self):
        user = self.id_entry.text()
        pw = self.pw_entry.text()
        addr = self.addr_entry.text()
        port = self.port_entry.text()
        endpoint = self.endpoint_entry.text()

        if user and pw:
            return f"rtsp://{user}:{pw}@{addr}:{port}/{endpoint}"
        else:
            return f"rtsp://{addr}:{port}/{endpoint}"

    def apply_and_close(self):
        new_url = self.get_url()
        set_camera_url(new_url)
        if hasattr(self.parent(), "cap"):
            self.parent().cap.release()
            self.parent().cap = cv2.VideoCapture(new_url)
        self.accept()

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("카메라 주소 설정")
        layout = QVBoxLayout()

        layout.addWidget(QLabel("ID:"))
        self.id_entry = QLineEdit()
        layout.addWidget(self.id_entry)

        layout.addWidget(QLabel("Password:"))
        self.pw_entry = QLineEdit()
        self.pw_entry.setEchoMode(QLineEdit.EchoMode.Password)
        layout.addWidget(self.pw_entry)

        layout.addWidget(QLabel("IP 주소:"))
        self.addr_entry = QLineEdit()
        layout.addWidget(self.addr_entry)

        layout.addWidget(QLabel("Port:"))
        self.port_entry = QLineEdit()
        self.port_entry.setText("554")
        layout.addWidget(self.port_entry)

        layout.addWidget(QLabel("Endpoint:"))
        self.endpoint_entry = QLineEdit()
        self.endpoint_entry.setText("onvif1")
        layout.addWidget(self.endpoint_entry)

        apply_btn = QPushButton("적용")
        apply_btn.clicked.connect(self.apply_and_close)
        layout.addWidget(apply_btn)

        self.setLayout(layout)

def start_monitoring():
    app = QApplication(sys.argv)
    cap = cv2.VideoCapture(current_camera_url)
    if not cap.isOpened():
        print("기본 CAMERA_RTSP 연결 실패, 설정창을 띄웁니다.")
        dialog = CameraConfigDialog()
        if dialog.exec():
            new_url = dialog.get_url()
            set_camera_url(new_url)
            print(f"새 카메라 주소로 갱신: {new_url}")
    cap.release()
    window = MonitorWindow()
    window.show()
    sys.exit(app.exec())
