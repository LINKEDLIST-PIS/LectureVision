📖 **LectureVision GUI**  
강의실 내 카메라, 모델 서버, API 서버와 연동되어 **실시간 인원 수, 화재 경보, 자원 상태**를 시각적으로 보여주는 통합 관리 GUI 프로그램입니다.  
PyQt5 기반으로 제작되었으며, 지도 탭 없이 실시간 알림 중심의 운영 화면을 제공합니다.  

---

📂 **프로젝트 구조**
```
LectureVision_GUI/
│
├─ main.py                ← GUI 실행 진입점
├─ lecturevision.kv       ← Kivy 레이아웃 정의 파일
│
├─ screens/
│   ├─ home_screen.py     ← 메인화면 (카메라 켜기, 인원수 보기 등)
│   ├─ settings_screen.py ← 설정화면 (서버 IP, 카메라 선택 등)
│
├─ utils/
│   ├─ camera_manager.py  ← 카메라 열기/닫기, 프레임 캡처 기능
│   ├─ api_client.py      ← 모델 서버와 통신하는 HTTP 클라이언트
│   ├─ config.py          ← 설정 저장 및 불러오기 (예: 서버 주소)
│
└─ assets/
    ├─ icons/             ← 버튼 아이콘 등
    └─ fonts/



