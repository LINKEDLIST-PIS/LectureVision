package com.son.lecture_project.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.son.lecture_project.data.local.TokenManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. 알림이 꺼져있으면 무시
        if (!TokenManager.areNotificationsEnabled()) {
            return
        }

        // 2. 인텐트로부터 수업 정보 추출
        val title = intent.getStringExtra("title") ?: "수업 알림"
        val message = intent.getStringExtra("message") ?: "곧 수업이 시작됩니다."

        // 3. 알림 표시
        NotificationHelper.showNotification(context, title, message)
    }
}
