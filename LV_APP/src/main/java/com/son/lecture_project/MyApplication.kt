package com.son.lecture_project

import android.app.Application
import com.son.lecture_project.data.local.TokenManager

/**
 * Custom Application class to perform one-time initializations.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the TokenManager with the application context
        TokenManager.init(this)
    }
}
