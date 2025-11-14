package com.son.lecture_project

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
// ======= 아래 4줄이 추가되었습니다! =======
import com.son.lecture_project.HomeFragment
import com.son.lecture_project.RecordsFragment
import com.son.lecture_project.SettingsFragment
import com.son.lecture_project.TimetableFragment
// ======================================
import com.son.lecture_project.databinding.ActivityBottomNavBinding

class BottomNavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    Toast.makeText(this, "알림 버튼이 클릭되었습니다.", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            // 이제 이 부분에서 오류가 사라집니다.
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_timetable -> TimetableFragment()
                R.id.nav_records -> RecordsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }

            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
