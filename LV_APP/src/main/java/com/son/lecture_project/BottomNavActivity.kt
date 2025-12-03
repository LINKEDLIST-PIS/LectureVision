package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.databinding.ActivityBottomNavBinding
import com.son.lecture_project.ui.home.HomeViewModel

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding
    private val homeViewModel: HomeViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusCheckRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 UI 상태 설정
        updateAllIndicators()
        updateBottomNavTitles()
        refreshTicketIndicatorVisibility()

        if (savedInstanceState == null) {
            replaceFragment(HomeScreen())
        }

        // 상단바 메뉴 설정
        setupTopAppBar()

        // 하단 네비게이션 리스너 설정
        setupBottomNavigation()
    }

    private fun setupStatusCheckRunnable() {
        statusCheckRunnable = Runnable {
            updateAllIndicators()
            handler.postDelayed(statusCheckRunnable, 1000) // 1초마다 반복
        }
    }

    override fun onResume() {
        super.onResume()
        setupStatusCheckRunnable()
        handler.post(statusCheckRunnable) // 주기적 상태 확인 시작
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusCheckRunnable) // 화면 벗어나면 중지
    }

    private fun updateAllIndicators() {
        // 토큰 상태 업데이트
        if (TokenManager.isTokenValid()) {
            binding.imgTokenStatus.setImageResource(R.drawable.indicator_green)
        } else {
            binding.imgTokenStatus.setImageResource(R.drawable.indicator_red)
        }

        // 티켓 상태 업데이트
        if (TokenManager.isTicketValid()) {
            binding.imgTicketStatus.setImageResource(R.drawable.indicator_square_green)
        } else {
            binding.imgTicketStatus.setImageResource(R.drawable.indicator_square_red)
        }
    }

    private fun updateBottomNavTitles() {
        binding.bottomNavigation.menu.findItem(R.id.nav_home)?.title = getString(R.string.nav_home)
        binding.bottomNavigation.menu.findItem(R.id.nav_timetable)?.title = getString(R.string.nav_timetable)
        binding.bottomNavigation.menu.findItem(R.id.nav_records)?.title = getString(R.string.nav_records)
        binding.bottomNavigation.menu.findItem(R.id.nav_settings)?.title = getString(R.string.nav_settings)
    }

    private fun setupTopAppBar() {
        val menuItem = binding.topAppBar.menu.findItem(R.id.action_notifications)
        val actionView = menuItem.actionView as FrameLayout?

        actionView?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        updateBadgeCount(actionView, 0) // 초기 뱃지 카운트

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notifications -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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

    fun refreshTicketIndicatorVisibility() {
        val isVisible = TokenManager.isTicketIndicatorVisible()
        binding.imgTicketStatus.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun updateBadgeCount(layout: View?, count: Int) {
        val badge = layout?.findViewById<TextView>(R.id.tv_notification_badge) ?: return
        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun replaceFragment(fragment: Fragment) {
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
