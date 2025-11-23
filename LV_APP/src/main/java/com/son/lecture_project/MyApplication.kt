package com.son.lecture_project

import android.app.Application
import com.son.lecture_project.data.local.TokenManager


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()


        TokenManager.init(this)
    }
}
