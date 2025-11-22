package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.son.lecture_project.databinding.ActivityBottomNavBinding
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding
    private val homeViewModel: HomeViewModel by viewModels() // ViewModel 공유

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 언어 변경 후 액티비티 재생성 시, 바텀 네비게이션 타이틀을 강제로 갱신
        updateBottomNavTitles()

        if (savedInstanceState == null) {
            replaceFragment(HomeScreen())
        }
        
        // 티켓 상태 관찰 시작
        observeTicketStatus()
        // 초기 상태 확인
        homeViewModel.checkTicketStatus()

        // 상단바 메뉴 설정 (커스텀 레이아웃 클릭 리스너 처리)
        val menuItem = binding.topAppBar.menu.findItem(R.id.action_notifications)
        val actionView = menuItem.actionView as FrameLayout?

        actionView?.let { layout ->
            layout.setOnClickListener {
                // 알림 화면으로 이동
                startActivity(Intent(this, NotificationActivity::class.java))
            }
            
            // 초기 뱃지 카운트는 0으로 설정 (알림이 없을 때는 숨김 처리됨)
            updateBadgeCount(layout, 0)
        }

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notifications -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            Log.d("BottomNav", "Selected Item: ${item.itemId}")

            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeScreen()
                R.id.nav_timetable -> TimetableScreen()
                R.id.nav_records -> RecordsScreen()
                R.id.nav_settings -> SettingsScreen()
                else -> HomeScreen()
            }

            replaceFragment(selectedFragment)
            true
        }
    }

    private fun updateBottomNavTitles() {
        binding.bottomNavigation.menu.findItem(R.id.nav_home)?.title = getString(R.string.nav_home)
        binding.bottomNavigation.menu.findItem(R.id.nav_timetable)?.title = getString(R.string.nav_timetable)
        binding.bottomNavigation.menu.findItem(R.id.nav_records)?.title = getString(R.string.nav_records)
        binding.bottomNavigation.menu.findItem(R.id.nav_settings)?.title = getString(R.string.nav_settings)
    }
    
    private fun observeTicketStatus() {
        homeViewModel.ticketStatus.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    val message = result.data
                    // '유효' 혹은 '보유'라는 단어가 포함되면 발급된 상태로 간주 (초록불)
                    if (message.contains("유효") || message.contains("보유")) {
                        binding.imgTicketStatus.setImageResource(R.drawable.indicator_green)
                    } else {
                        binding.imgTicketStatus.setImageResource(R.drawable.indicator_red)
                    }
                }
                else -> {
                    // 로딩 중이거나 에러 발생 시 기본적으로 빨간불 (혹은 노란불 고려 가능)
                    binding.imgTicketStatus.setImageResource(R.drawable.indicator_red)
                }
            }
        }
    }

    // 뱃지 카운트 업데이트 함수
    // 외부에서 호출하여 알림 개수를 갱신할 수 있음
    fun updateBadgeCount(layout: View, count: Int) {
        val badge = layout.findViewById<TextView>(R.id.tv_notification_badge) ?: return
        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d("BottomNav", "Replacing fragment with ${fragment::class.java.simpleName}")
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("BottomNav", "Error replacing fragment", e)
            Toast.makeText(this, "화면 이동 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }
}
