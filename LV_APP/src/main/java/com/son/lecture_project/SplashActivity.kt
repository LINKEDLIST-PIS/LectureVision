package com.son.lecture_project

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.LoginRequest
import kotlinx.coroutines.launch

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
                TokenManager.clearAllData()
                TokenManager.saveAppVersion(currentVersion)
                navigateToLogin()
            } else {
                // 4. 저장된 계정 정보로 자동 로그인 시도
                tryAutoLogin()
            }

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            navigateToLogin()
        }
    }

    private fun tryAutoLogin() {
        val email = TokenManager.getUserEmail()
        val password = TokenManager.getUserPassword()

        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            // 저장된 정보가 없으면 로그인 화면으로
            navigateToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                // 저장된 ID/PW로 로그인 API 호출
                val loginRequest = LoginRequest(email, password)
                val response = RetrofitClient.mainApiService.login(loginRequest)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    // 새 토큰 저장
                    TokenManager.saveToken(loginResponse.accessToken)
                    Log.d("SplashActivity", "Auto-login successful")
                    navigateToMain()
                } else {
                    // 로그인 실패 (비번 변경 등) -> 수동 로그인 유도
                    Log.w("SplashActivity", "Auto-login failed: ${response.code()}")
                    TokenManager.clearToken() // 기존 토큰 삭제
                    navigateToLogin()
                }
            } catch (e: Exception) {
                // 네트워크 오류 등 -> 수동 로그인 유도
                Log.e("SplashActivity", "Error during auto-login", e)
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, BottomNavActivity::class.java))
        finish()
    }
}
