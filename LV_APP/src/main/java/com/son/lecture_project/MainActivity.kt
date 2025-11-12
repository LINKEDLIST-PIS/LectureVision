package com.son.lecture_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.son.lecture_project.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 앱이 처음 시작될 때 HomeFragment를 기본 화면으로 설정합니다.
        //    (savedInstanceState == null 은 최초 실행 시에만 동작하도록 하는 조건)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // 2. 바텀 네비게이션의 각 탭을 눌렀을 때의 동작을 설정합니다.
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // '홈' 탭을 누르면 HomeFragment로 교체합니다.
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true // 이벤트 처리가 성공했음을 알립니다.
                }

                // TODO: 앞으로 다른 프래그먼트를 만들면 여기에 추가합니다.
                // R.id.nav_timetable -> {
                //     replaceFragment(TimetableFragment()) // 예시
                //     true
                // }
                // R.id.nav_records -> {
                //     replaceFragment(RecordsFragment()) // 예시
                //     true
                // }

                else -> false // 처리할 아이템이 없으면 false를 반환합니다.
            }
        }
    }

    // 3. 화면의 특정 영역(FrameLayout)을 다른 프래그먼트로 교체하는 함수입니다.
    private fun replaceFragment(fragment: Fragment) {
        // supportFragmentManager를 통해 프래그먼트 교체 작업을 시작합니다.
        supportFragmentManager.beginTransaction()
            // R.id.main_container 라는 ID를 가진 뷰를 'fragment'로 교체합니다.
            // ※ 주의: activity_main.xml에 이 ID를 가진 FrameLayout이 있어야 합니다!
            .replace(R.id.main_container, fragment)
            .commit() // 작업을 완료합니다.
    }
}
