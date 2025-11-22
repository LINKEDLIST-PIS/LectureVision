package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.son.lecture_project.data.local.TokenManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // A short delay to show the splash screen, then decide where to go.
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if a token exists
            val token = TokenManager.getToken()

            // 토큰이 있으면 메인 화면(BottomNavActivity), 없으면 로그인 화면(LoginActivity)으로 이동
            val nextActivity = if (!token.isNullOrEmpty()) {
                BottomNavActivity::class.java
            } else {
                LoginActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish() // Finish SplashActivity so the user can't go back to it

        }, 1000) // Reduced delay to 1 second for a faster app start
    }
}
