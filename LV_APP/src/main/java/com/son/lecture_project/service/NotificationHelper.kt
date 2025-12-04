package com.son.lecture_project.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.son.lecture_project.R

object NotificationHelper {

    private const val CHANNEL_ID = "lecture_channel"
    private const val CHANNEL_NAME = "수업 알림"
    private const val CHANNEL_DESCRIPTION = "수업 시작 및 주요 이벤트 알림"

    // 1. 알림 채널 생성 (앱 시작 시 호출)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 2. 알림 생성 및 반환
    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 알림에는 작은 아이콘이 필수입니다. 시스템 기본 아이콘으로 설정했습니다.
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // 알림 ID는 각 알림을 구별하기 위해 사용 (여기서는 단순하게 고정값 사용)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
