📖 **LectureVision GUI**  
강의실 내 카메라, 모델 서버, API 서버와 연동되어 **실시간 인원 수, 화재 경보, 자원 상태**를 시각적으로 보여주는 통합 관리 GUI 프로그램입니다.  
PyQt5 기반으로 제작되었으며, 지도 탭 없이 실시간 알림 중심의 운영 화면을 제공합니다.  

---

📂 **프로젝트 구조**
```
LectureVision/
│
├─ main.py                 # 앱 실행 및 ScreenManager
├─ kv/
│   └─ lecture_vision.kv   # 전체 UI
├─ screens/
│   ├─ __init__.py
│   ├─ login_screen.py           # [로그인 기능]
│   ├─ signup_screen.py          # [회원가입 기능]
│   ├─ measure_screen.py         # [인원수 측정 기능]
│   ├─ compare_screen.py         # [측정 인원 비교 기능]
│   ├─ record_screen.py          # [인원수 기록 조회 기능]
│   └─ timer_screen.py           # [재측정 기능 / 타이머 기능]
├─ utils/
│   ├─ __init__.py
│   ├─ file_io.py                # [파일 입출력, JSON I/O]
│   └─ api.py                    # [API 연동 / 서버 통신]
└─ data/
    ├─ users.json
    ├─ timetable.json
    └─ records.json







<파일별 역활>

| 파일                | 기능        | 설명 (한국어)                     |
| ----------------- | --------- | ---------------------------- |
| login_screen.py   | 로그인       | 이메일과 비밀번호로 로그인 처리            |
| signup_screen.py  | 회원가입      | 이름, 전화번호, 이메일, 비밀번호 등록       |
| measure_screen.py | 인원수 측정    | 수동 입력, 카메라 촬영 후 서버 업로드 측정    |
| compare_screen.py | 측정 인원 비교  | 현재 측정 인원과 기존 기록 비교 (자동 업데이트) |
| record_screen.py  | 인원수 기록 조회 | 과거 기록 확인, CSV 내보내기, 삭제 기능    |
| timer_screen.py   | 재측정 / 타이머 | 설정 시간마다 자동 측정, 상태 표시         |
| file_io.py        | 파일 입출력    | JSON 로딩/저장, 데이터 디렉토리 관리      |
| api.py            | API 연동    | 서버 업로드, JSON 응답 처리           |
