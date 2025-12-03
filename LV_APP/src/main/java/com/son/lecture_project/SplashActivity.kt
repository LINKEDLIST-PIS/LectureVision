package com.son.lecture_project

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.son.lecture_project.data.api.RetrofitClient
import com.son.lecture_project.data.local.TokenManager
import com.son.lecture_project.data.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // 앱 시작 시, 시스템 설정과 앱 설정을 동기화
        syncThemeWithSystem()

        // 1초 후 버전 확인 및 화면 이동 로직 실행
        Handler(Looper.getMainLooper()).postDelayed({
            checkAppVersionAndNavigate()
        }, 1000)
    }

    private fun syncThemeWithSystem() {
        // 현재 시스템의 다크 모드 상태 확인
        val isSystemNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // 앱에 저장된 다크 모드 설정 값
        val isAppDarkMode = TokenManager.isDarkMode()

        // 두 설정이 일치하지 않을 경우, 시스템 설정을 따라 앱 설정을 업데이트
        if (isAppDarkMode != isSystemNightMode) {
            TokenManager.setDarkMode(isSystemNightMode)
        }

        // 최종 결정된 모드를 앱 전체에 적용
        val mode = if (TokenManager.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun checkAppVersionAndNavigate() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0.0"
            val savedVersion = TokenManager.getSavedAppVersion()

            if (savedVersion != currentVersion) {
                TokenManager.clearAllData()
                TokenManager.saveAppVersion(currentVersion)
                navigateToLogin()
            } else {
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
            navigateToLogin()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) { 
            try {
                val loginRequest = LoginRequest(email, password)
                val response = RetrofitClient.mainApiService.login(loginRequest)

                withContext(Dispatchers.Main) { 
                    if (response.isSuccessful && response.body() != null) {
                        TokenManager.saveToken(response.body()!!.accessToken)
                        navigateToMain()
                    } else {
                        TokenManager.clearToken()
                        navigateToLogin()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    Log.e("SplashActivity", "Error during auto-login", e)
                    navigateToLogin()
                }
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
