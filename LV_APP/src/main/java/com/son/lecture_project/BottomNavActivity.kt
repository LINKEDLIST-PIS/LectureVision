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
import com.son.lecture_project.data.NotificationStorage
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.ClassSchedule
import com.son.lecture_project.databinding.ActivityBottomNavBinding
import com.son.lecture_project.service.NotificationHelper
import com.son.lecture_project.ui.home.HomeViewModel
import com.son.lecture_project.ui.home.Result
import java.util.Calendar
import java.util.TimeZone

class BottomNavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomNavBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusCheckRunnable: Runnable
    private lateinit var notificationCheckRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateAllIndicators()
        updateBottomNavTitles()
        refreshTicketIndicatorVisibility()

        if (savedInstanceState == null) {
            replaceFragment(HomeScreen())
        }

        setupTopAppBar()
        setupBottomNavigation()

        notificationViewModel.loadNotifications()
        observeNotificationBadge()
    }

    private fun setupRunnables() {
        statusCheckRunnable = Runnable {
            updateAllIndicators()
            handler.postDelayed(statusCheckRunnable, 1000) // 1초마다 반복
        }

        notificationCheckRunnable = Runnable {
            Log.d("BottomNavActivity", "Checking for class notifications...")
            checkForUpcomingClasses()
            handler.postDelayed(notificationCheckRunnable, 60 * 1000) // 1분마다 반복
        }
    }

    override fun onResume() {
        super.onResume()
        setupRunnables()
        handler.post(statusCheckRunnable)
        handler.post(notificationCheckRunnable) // 알림 체크 시작
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusCheckRunnable)
        handler.removeCallbacks(notificationCheckRunnable) // 알림 체크 중지
    }

    private fun checkForUpcomingClasses() {
        val todayClassesResult = homeViewModel.todayClasses.value
        if (todayClassesResult is Result.Success) {
            val schedules = todayClassesResult.data
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            val currentTimeInMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            
            val todayName = getTodayDayName(cal)

            schedules.forEach { schedule ->
                // 1. 오늘 요일의 수업인지 확인 (schedule.day는 "월", "화" 등의 문자열)
                if (!schedule.day.contains(todayName)) return@forEach
                
                // 2. 수업 시작 시간 (분 단위) 계산
                val startTimeInMinutes = parseTimeToMinutes(schedule.startTime)
                if (startTimeInMinutes == -1) return@forEach

                // 3. 알림 시간 (수업 10분 전)인지 확인
                val notificationTime = startTimeInMinutes - 10
                if (currentTimeInMinutes == notificationTime) {
                    
                    // 4. 중복 알림 방지
                    val notificationKey = "${schedule.name}-${schedule.startTime}"
                    if (!NotificationStorage.isNotificationSent(notificationKey)) {
                        
                        // 5. 알림 생성 및 발송
                        val title = schedule.name
                        val content = "${schedule.startTime}~${schedule.endTime} (총 ${schedule.totalStudents}명) - 수업 10분 전입니다."
                        
                        NotificationHelper.showNotification(this, title, content)
                        NotificationStorage.addNotification(title, content)
                        NotificationStorage.setNotificationSent(notificationKey)

                        Log.i("BottomNavActivity", "Notification sent for class: ${schedule.name}")
                    }
                }
            }
        }
    }
    
    private fun getTodayDayName(calendar: Calendar): String {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + (parts.getOrNull(1)?.toInt() ?: 0)
        } catch (e: Exception) {
            -1
        }
    }

    private fun updateAllIndicators() {
        if (TokenManager.isTokenValid()) {
            binding.imgTokenStatus.setImageResource(R.drawable.indicator_green)
        } else {
            binding.imgTokenStatus.setImageResource(R.drawable.indicator_red)
        }

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
            NotificationStorage.clearNotifications() // 알림 목록 확인 시 뱃지 초기화
        }
        updateBadgeCount(actionView, 0)

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_notifications -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    NotificationStorage.clearNotifications() // 알림 목록 확인 시 뱃지 초기화
                    true
                }
                else -> false
            }
        }
    }
    
    private fun observeNotificationBadge() {
        notificationViewModel.notifications.observe(this) { items ->
            val menuItem = binding.topAppBar.menu.findItem(R.id.action_notifications)
            val actionView = menuItem.actionView as FrameLayout?
            updateBadgeCount(actionView, items.size)
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
