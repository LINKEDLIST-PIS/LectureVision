package com.son.lecture_project

import android.app.Application
import android.content.Context
import com.son.lecture_project.data.local.TokenManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        TokenManager.init(appContext)
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
