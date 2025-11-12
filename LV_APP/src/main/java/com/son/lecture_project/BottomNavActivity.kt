package com.son.lecture_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.son.lecture_project.databinding.ActivityBottomNavBinding

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱 시작 시 첫 화면으로 HomeFragment를 설정
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment()) // 오류 수정
                .commit()
        }

        // 하단 네비게이션 아이템 클릭 리스너 설정
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment() // 오류 수정
                // TODO: 시간표, 기록, 설정 Fragment를 생성하고 아래 주석을 해제하여 연결합니다.
                // R.id.nav_timetable -> selectedFragment = TimetableFragment()
                // R.id.nav_records -> selectedFragment = RecordsFragment()
                // R.id.nav_settings -> selectedFragment = SettingsFragment()
            }

            // 선택된 Fragment가 있으면 화면을 교체
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
                true
            } else {
                false
            }
        }
    }
}
