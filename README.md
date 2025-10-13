📖 **LectureVision GUI**  
강의실 내 카메라, 모델 서버, API 서버와 연동되어 **실시간 인원 수, 화재 경보, 자원 상태**를 시각적으로 보여주는 통합 관리 GUI 프로그램입니다.  
PyQt5 기반으로 제작되었으며, 지도 탭 없이 실시간 알림 중심의 운영 화면을 제공합니다.  

---

📂 **프로젝트 구조**
```
project-root/
 └── gui/
      ├── __init__.py          # 패키지 초기화
      ├── main.py              # 프로그램 진입점 (QApplication 실행)
      ├── dashboard.py         # 메인 대시보드 UI
      ├── components/
      │    ├── alerts_panel.py     # 실시간 알림 표시 패널
      │    ├── charts_panel.py     # 통계 차트 (바, 라인, 파이)
      │    ├── resource_panel.py   # 장비 상태 및 배치 정보
      │    └── controls_panel.py   # 수동 모드 / 자동 모드 전환 버튼
      ├── network/
      │    ├── api_client.py       # 모델 서버/DB 서버 통신 모듈
      │    └── websocket_client.py # 실시간 이벤트 스트림 수신
      ├── utils/
      │    ├── styles.py           # 공통 스타일, 색상 테마
      │    ├── logger.py           # 로그 출력 및 파일 저장
      │    └── config.py           # 서버 주소, 포트, 토큰 등 환경 설정
      └── assets/
           ├── icons/              # 버튼 및 알림 아이콘
           └── fonts/              # UI 폰트 리소스
```

---

⚙️ **실행 방법**

1️⃣ **환경 설정**  
`utils/config.py` 파일에서 서버 정보 수정:
```python
MODEL_SERVER_URL = "http://127.0.0.1:8000"
API_SERVER_URL = "https://example.com/api"
API_TOKEN = "your_token_here"
```

2️⃣ **프로그램 실행**
```bash
python -m gui.main
```

3️⃣ **실행 후 주요 기능**
- **상단:** 현재 시각, 연결 상태, 모드(자동/수동) 표시  
- **좌측:** 실시간 경보 리스트 (화재 감지, 인원 급증 등)  
- **중앙:** 현재 카메라 영상 + 사람 수 표시  
 
- **하단:** 최근 로그 및 서버 응답 상태 표시  

---

📊 **주요 기능 흐름**

| 모듈 | 기능 |
|------|------|
| `network/api_client.py` | FastAPI 모델 서버에 인원 수 
| `websocket_client.py` | 서버로부터 실시간 이벤트 수신 |
| `dashboard.py` | 주요 위젯 배치 및 화면 갱신 제어 |
| `alerts_panel.py` | 탐지 이벤트 발생 시 즉시 알림 표시 |
| `charts_panel.py` | 시간대별 인원 변동 및 위험 등급 시각화 |
| `resource_panel.py` | 자원(소방차, 드론 등) 상태 업데이트 |
| `controls_panel.py` | 자동/수동 모드 전환, 수동 경보 발령 |

---

🔒 **보안 및 통신**

- **모델 서버 ↔ GUI**
  - HTTPS 기반 REST API  
  - HMAC-SHA256 서명 검증  
- **이벤트 스트림**
  - WebSocket 실시간 데이터 수신  
  - Heartbeat 체크를 통해 연결 상태 자동 복구  

---

🎯 **운영 흐름**
```
모델 서버 (사람 감지) 
   ↓
API 서버 (데이터 업로드 및 저장)
   ↓
LectureVision GUI (시각화 + 제어)
```

