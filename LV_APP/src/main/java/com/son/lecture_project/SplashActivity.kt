package com.son.lecture_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // 안전하게 findViewById 하고 기존 패딩 보존
        val mainView: View? = findViewById(R.id.main)
        mainView?.let { v ->
            val originalLeft = v.paddingLeft
            val originalTop = v.paddingTop
            val originalRight = v.paddingRight
            val originalBottom = v.paddingBottom

            ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    originalLeft + systemBars.left,
                    originalTop + systemBars.top,
                    originalRight + systemBars.right,
                    originalBottom + systemBars.bottom
                )
                insets
            }
        }

        // ✅ 일정 시간(2초) 지연 후 MainActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 뒤로가기 시 다시 안 돌아오게 종료
        }, 5000)
    }
}
