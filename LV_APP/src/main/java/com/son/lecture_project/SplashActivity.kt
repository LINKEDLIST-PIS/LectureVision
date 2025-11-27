package com.son.lecture_project

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.son.lecture_project.data.local.TokenManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 앱 초기화 및 버전 확인
        Handler(Looper.getMainLooper()).postDelayed({
            checkAppVersionAndNavigate()
        }, 1000)
    }

    private fun checkAppVersionAndNavigate() {
        try {
            // 1. 현재 설치된 앱 버전 가져오기
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0.0"

            // 2. 캐시에 저장된 이전 실행 시의 앱 버전 가져오기
            val savedVersion = TokenManager.getSavedAppVersion()

            Log.d("SplashActivity", "Current: $currentVersion, Saved: $savedVersion")

            if (savedVersion != currentVersion) {
                // 3. 버전이 다르거나 처음 실행인 경우 (업데이트 감지)
                Log.d("SplashActivity", "Version mismatch! Clearing cache.")
                
                // 기존 캐시 데이터 모두 삭제
                TokenManager.clearAllData()
                
                // 새로운 버전 정보 저장
                TokenManager.saveAppVersion(currentVersion)
                
                // 4. 데이터가 초기화되었으므로 무조건 로그인 화면으로 이동
                navigateToLogin()
            } else {
                // 5. 버전이 같으면 기존 로직대로 토큰 확인 후 이동
                checkTokenAndNavigate()
            }

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            // 에러 발생 시 안전하게 로그인 화면으로
            navigateToLogin()
        }
    }

    private fun checkTokenAndNavigate() {
        val token = TokenManager.getToken()
        
        // 토큰이 있으면 메인, 없으면 로그인
        val nextActivity = if (!token.isNullOrEmpty()) {
            BottomNavActivity::class.java
        } else {
            LoginActivity::class.java
        }
        
        startActivity(Intent(this, nextActivity))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
