---
📂 프로젝트 구조
```
com.son.lecture_project
├── MyApplication.kt           // 앱의 전역 상태 관리 및 초기화
├── SplashActivity.kt          // 앱 시작 화면 (자동 로그인 처리)
├── LoginActivity.kt           // 로그인 화면
├── SignupActivity.kt          // 회원가입 화면
├── BottomNavActivity.kt       // 메인 화면 (하단 탭 네비게이션 컨테이너)
├── NotificationActivity.kt    // 알림 내역 화면
├── TimetableView.kt           // 커스텀 시간표 뷰
│
├── data                       // [데이터 계층] 서버 통신 및 로컬 저장
│   ├── api
│   │   ├── ApiClient.kt           // Retrofit 인스턴스 생성 (Base URL 설정)
│   │   ├── MainApiService.kt      // 메인 서버 API 인터페이스 (로그인, 유저정보 등)
│   │   ├── ModelApiService.kt     // AI 모델 서버 API 인터페이스 (인원 측정 등)
│   │   └── RetrofitClient.kt      // API 서비스 객체 제공자
│   ├── local
│   │   └── TokenManager.kt        // SharedPreferences 관리 (토큰, 사용자 정보 저장)
│   └── model
│       ├── LoginRequest.kt, Notice.kt, Ticket.kt 등 // 데이터 클래스 (DTO)
│
└── ui                         // [UI 계층] 화면 별 ViewModel 및 로직
    ├── auth
    │   ├── LoginViewModel.kt      // 로그인 비즈니스 로직
    │   └── SignupViewModel.kt     // 회원가입 비즈니스 로직
    ├── home
    │   ├── HomeScreen.kt          // [탭1] 홈 화면 (출석 체크, 공지사항)
    │   └── HomeViewModel.kt       // 홈 화면 데이터 관리 (티켓, 타이머, 공지)
    ├── timetable
    │   ├── TimetableScreen.kt     // [탭2] 시간표 화면
    │   └── TimetableViewModel.kt  // 시간표 데이터 관리 (CRUD)
    ├── records
    │   ├── RecordsScreen.kt       // [탭3] 기록 화면 (출석 이력)
    │   └── RecordsViewModel.kt    // 기록 데이터 로딩
    └── settings
        └── SettingsScreen.kt      // [탭4] 설정 화면 (언어, 비밀번호 변경)
```
📱 LectureVision 앱 구조 개요
---
🚀 A. 앱 진입 및 인증 (Entry & Auth)
---
MyApplication.kt

앱 실행 시 가장 먼저 호출되는 클래스

TokenManager.init(this)를 통해 SharedPreferences 전역 초기화

SplashActivity.kt

앱 첫 실행 시 스플래시 화면 표시

저장된 토큰 확인

로그인 상태 → BottomNavActivity 이동

비로그인 상태 → LoginActivity 이동

LoginActivity.kt / LoginViewModel.kt

이메일과 비밀번호 입력 후 서버에 로그인 요청

로그인 성공 시

Access Token 저장

사용자 ID 저장

메인 화면 이동

SignupActivity.kt / SignupViewModel.kt

이름, 이메일(@gnu.ac.kr), 비밀번호 입력

유효성 검사 후 회원가입 요청

🧭 B. 메인 컨테이너 (Main Container)
---
BottomNavActivity.kt

로그인 이후 진입하는 메인 액티비티

홈 / 시간표 / 기록 / 설정 프래그먼트 전환

TopAppBar에 알림 아이콘 및 읽지 않은 알림 배지 표시

📂 C. 주요 탭 화면 (Fragments)
---
🏠 HomeScreen.kt / HomeViewModel.kt

역할: 앱 대시보드
기능:

티켓 상태 조회 및 없을 경우 자동 발급

타이머 → AI 모델 서버에 인원 측정 요청

현재 강의실 인원 표시

최신 공지 표시

오늘의 시간표 요약

📅 TimetableScreen.kt / TimetableViewModel.kt

역할: 시간표 관리
기능:

TimetableView 커스텀 뷰로 주간 시간표 렌더링

수업 추가·수정·삭제

강의 색상 선택

데이터 로컬/서버 저장

🧾 RecordsScreen.kt / RecordsViewModel.kt

역할: 사용자 활동 기록 조회
기능:

서버에서 기록 데이터 가져오기

7일/30일 기간 필터

과목별 필터

추후 통계 보기 기능 제공 예정

⚙️ SettingsScreen.kt

역할: 앱 설정 및 계정 관련 기능
기능:

비밀번호 변경

이메일 변경

로그아웃

언어 변경(한국어/영어)

알림 수신 설정

🌐 D. 데이터 & 네트워크 (Data Layer)
---
ApiClient.kt / RetrofitClient.kt

Retrofit 기반 HTTP 통신

서로 다른 Base URL 관리

MainApiService (메인 서버)

ModelApiService (AI 모델 서버)

TokenManager.kt

SharedPreferences 기반 싱글톤

토큰 및 사용자 정보 저장

Access Token

사용자 ID

이메일

언어 설정

알림 설정

🛠 E. 유틸리티 및 커스텀 뷰
---
TimetableView.kt

Canvas 기반 커스텀 뷰

시간표 격자 및 수업 블록 렌더링

터치 시 선택된 수업 정보 반환

NotificationActivity.kt

알림 아이콘 클릭 시 이동

서버 또는 로컬에 저장된 알림 목록 표시
