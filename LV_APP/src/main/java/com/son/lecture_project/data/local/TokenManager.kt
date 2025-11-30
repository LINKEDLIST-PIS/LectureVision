package com.son.lecture_project.data.local

import android.content.Context
import android.content.SharedPreferences


object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password" // [추가] 자동 로그인을 위한 비밀번호 저장
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // 설정 관련 키
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TICKET_INDICATOR = "ticket_indicator"
    
    // 디버그 및 모델 서버 관련 키
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_MODEL_SERVER_URL = "model_server_url"

    // 앱 버전 관리 키
    private const val KEY_APP_VERSION = "app_version_cache"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        setLoggedIn(true)
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    // [추가] 비밀번호 저장 및 로드 (암호화되지 않은 상태로 저장되므로 보안에 취약할 수 있음 - 주의 필요)
    // 실제 프로덕션 앱에서는 EncryptedSharedPreferences를 사용해야 합니다.
    fun saveUserPassword(password: String) {
        prefs.edit().putString(KEY_USER_PASSWORD, password).apply()
    }

    fun getUserPassword(): String? {
        return prefs.getString(KEY_USER_PASSWORD, null)
    }

    // --- 설정 관련 메서드 ---

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }
    
    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
    }
    
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "ko") ?: "ko"
    }

    fun setTicketIndicatorVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_TICKET_INDICATOR, visible).apply()
    }

    fun isTicketIndicatorVisible(): Boolean {
        return prefs.getBoolean(KEY_TICKET_INDICATOR, true)
    }
    
    // --- 디버그 모드 및 모델 서버 URL ---
    
    fun setDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
    }
    
    fun isDebugMode(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_MODE, false)
    }
    
    fun setModelServerUrl(url: String) {
        prefs.edit().putString(KEY_MODEL_SERVER_URL, url).apply()
    }
    
    fun getModelServerUrl(): String? {
        return prefs.getString(KEY_MODEL_SERVER_URL, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
        setLoggedIn(false)
    }

    fun getSavedAppVersion(): String? {
        return prefs.getString(KEY_APP_VERSION, null)
    }

    fun saveAppVersion(version: String) {
        prefs.edit().putString(KEY_APP_VERSION, version).apply()
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        setLoggedIn(false)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
}
