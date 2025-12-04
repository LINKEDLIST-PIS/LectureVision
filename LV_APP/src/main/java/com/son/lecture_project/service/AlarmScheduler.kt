package com.son.lecture_project.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.son.lecture_project.data.model.ClassSchedule
import java.util.Calendar
import java.util.TimeZone

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleClassAlarms(context: Context, schedules: List<ClassSchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 기존 알람 취소 로직은 구현이 복잡하므로, 여기서는 새로 등록하는 것만 처리
        // 실제 앱에서는 기존 알람을 모두 취소하고 다시 등록하거나, ID 관리를 해야 함

        schedules.forEach { schedule ->
            val calendar = getNextClassTime(schedule) ?: return@forEach

            // 수업 10분 전으로 설정
            calendar.add(Calendar.MINUTE, -10)

            // 만약 10분 전 시간이 이미 지났다면 다음 주로 예약
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", "수업 알림")
                putExtra("message", "잠시 후 ${schedule.startTime}부터 '${schedule.name}' 수업이 시작됩니다.")
            }

            // 각 수업마다 고유한 ID 생성 (요일 + 시작시간 해시 등 활용)
            val alarmId = (schedule.day + schedule.startTime + schedule.name).hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 정확한 시간에 알람 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d("AlarmScheduler", "Scheduled alarm for ${schedule.name} at ${calendar.time}")
        }
    }
    
    // 요일 문자열(월, 화...)을 Calendar 상수로 변환
    private fun getDayOfWeek(dayString: String): Int {
        return when {
            dayString.contains("일") -> Calendar.SUNDAY
            dayString.contains("월") -> Calendar.MONDAY
            dayString.contains("화") -> Calendar.TUESDAY
            dayString.contains("수") -> Calendar.WEDNESDAY
            dayString.contains("목") -> Calendar.THURSDAY
            dayString.contains("금") -> Calendar.FRIDAY
            dayString.contains("토") -> Calendar.SATURDAY
            else -> -1
        }
    }

    private fun getNextClassTime(schedule: ClassSchedule): Calendar? {
        val dayOfWeek = getDayOfWeek(schedule.day)
        if (dayOfWeek == -1) return null

        val timeParts = schedule.startTime.split(":")
        if (timeParts.size < 2) return null

        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }

        // 설정한 시간이 현재보다 이전이면 다음주로 넘김
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return calendar
    }
}
