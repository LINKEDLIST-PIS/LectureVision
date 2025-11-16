📁 프로젝트 구조
app/
 └── src/
      └── main/
           ├── java/com/son/lecture_project/
           │    ├── BottomNavigationActivity.kt   # ✅ 앱의 메인 뼈대(틀) 액티비티
           │    ├── HomeFragment.kt               # 🏠 1. 홈 화면
           │    ├── TimetableFragment.kt          # 🗓️ 2. 시간표 화면
           │    ├── RecordsFragment.kt            # 📝 3. 기록 화면
           │    └── SettingsFragment.kt           # ⚙️ 4. 설정 화면
           │
           └── res/
                ├── layout/
                │    ├── activity_bottom_nav.xml   # ✅ 하단 네비게이션 포함 메인 레이아웃
                │    ├── fragment_home.xml         # 홈 화면 UI
                │    ├── fragment_timetable.xml    # 시간표 화면 UI
                │    ├── fragment_records.xml      # 기록 화면 UI
                │    └── fragment_settings.xml     # 설정 화면 UI
                │
                ├── menu/
                │    └── bottom_nav_menu.xml       # 하단 네비게이션 메뉴 아이템 정의
                │
                └── color/
                     └── nav_item_color.xml        # 네비게이션 아이콘/텍스트 컬러 상태 정의

