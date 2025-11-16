
📂 프로젝트 구조
app/
 └── src/
      └── main/
           ├── java/com/son/lecture_project/
           │    ├── BottomNavigationActivity.kt  # ✅ 메인 '틀' 액티비티
           │    ├── HomeFragment.kt              # 1. 홈 화면 조각
           │    ├── TimetableFragment.kt         # 2. 시간표 화면 조각
           │    ├── RecordsFragment.kt           # 3. 기록 화면 조각
           │    └── SettingsFragment.kt          # 4. 설정 화면 조각
           │
           └── res/
                ├── layout/
                │    ├── activity_bottom_nav.xml   # ✅ 메인 '틀' 레이아웃
                │    ├── fragment_home.xml         # 1. 홈 화면 레이아웃
                │    ├── fragment_timetable.xml    # 2. 시간표 화면 레이아웃
                │    ├── fragment_records.xml      # 3. 기록 화면 레이아웃
                │    └── fragment_settings.xml     # 4. 설정 화면 레이아웃
                │
                ├── menu/
                │    └── bottom_nav_menu.xml       # 하단 네비게이션 메뉴 아이템 정의
                │
                └── color/
                     └── nav_item_color.xml        # 하단 네비게이션 아이콘/텍스트 색상 정의

