---

# 📖 LectureVision Model Server

강의실 환경에서 **실시간 사람 수 탐지 및 모자이크 처리**를 수행하는 모델 서버입니다.  
YOLOv11M 기반 객체 탐지 모델을 사용하여 카메라 입력에서 사람을 인식하고, 개인정보 보호를 위해 모자이크 처리한 뒤 API 서버 업로드 기능 및 모니터링 화면을 제공합니다.  

---

## 📂 프로젝트 구조
```
project-root/
 └── model/
      ├── __init__.py       # 패키지 초기화
      ├── config.py         # 환경 설정 (RTSP, API 토큰 등)
      ├── camera.py         # 카메라 입력 모듈
      ├── detector.py       # YOLO 모델 로딩 및 사람 탐지
      ├── processing.py     # 모자이크 후처리
      ├── uploader.py       # API 업로드 (HMAC 인증 포함)
      ├── monitor.py        # 실시간 모니터링 GUI
      └── main.py           # FastAPI 서버 엔트리포인트
```

---

## ⚙️ 실행 방법

### 1. 환경 설정
`model/config.py` 파일에서 환경 변수를 수정합니다:
```python
CAMERA_RTSP = "rtsp://user:pass@ip:port/stream"
DEVICE = "cuda"  # or "cpu"
CONF_THRESHOLD = 0.5

API_BASE = "https://example.com/api"
API_TOKEN = "your_api_token"
HMAC_SECRET = "your_hmac_secret"
```
보안을 위해 실제 구성 내용은 .env를 통해 따로 관리합니다.

### 2. 서버 실행
```bash
python -m model.main
```

- FastAPI 서버: `http://localhost:8000`  
- `/measure` 엔드포인트: 카메라에서 프레임 캡처 → 사람 탐지 → 모자이크 → 업로드 → JSON 응답  

### 3. 모니터링
`main.py` 실행 시 `monitor.py`가 스레드로 실행되어,  
OpenCV 창에서 실시간 탐지 결과(사람 수 + 모자이크 영상)를 확인할 수 있습니다.  

---

## 📡 API 사용 예시

### 요청
```bash
curl -X POST http://localhost:8000/measure
```

### 응답
```json
{
  "people_count": 3,
  "api_response": {
    "status": "ok",
    "id": "abc123"
  }
}
```

---

🔒 보안
- 업로드 시 Bearer 토큰 인증 + HMAC-SHA256 서명을 함께 사용
- 요청 헤더:
- `Authorization: Bearer <API_TOKEN>`
- `X-Timestamp: <epoch time>`
- `X-Signature: <HMAC-SHA256 signature>`
- `Idempotency-Key: <unique key>`
- 이를 통해 인증 + 무결성 + 재전송 방지를 동시에 보장 

---

## 👀 처리 흐름
1. `camera.py` → 카메라 프레임 캡처  
2. `detector.py` → YOLOv11M 모델로 사람 탐지  
3. `processing.py` → 탐지된 영역 모자이크 처리  
4. `uploader.py` → 결과 이미지 + 사람 수 업로드  
5. `main.py` → FastAPI 엔드포인트 `/measure` 제공  
6. `monitor.py` → OpenCV GUI로 실시간 모니터링  

---
