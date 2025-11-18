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

            // Decide the next activity based on token availability
            val nextActivity = if (token != null) {
                // If token exists, go to the main activity
                BottomNavActivity::class.java
            } else {
                // If not, go to the login activity
                LoginActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish() // Finish SplashActivity so the user can't go back to it

        }, 1000) // Reduced delay to 1 second for a faster app start
    }
}
