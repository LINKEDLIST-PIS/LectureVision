package com.son.lecture_project

import android.app.Application
import android.content.Context
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.service.NotificationHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        TokenManager.init(appContext)
        
        // 알림 채널 생성 (안드로이드 8.0 이상 필수)
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
