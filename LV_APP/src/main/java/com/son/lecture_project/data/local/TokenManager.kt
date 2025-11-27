package com.son.lecture_project.data.local

import android.content.Context
import android.content.SharedPreferences


object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    
    // 설정 관련 키
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TICKET_INDICATOR = "ticket_indicator"
    
    // 디버그 및 모델 서버 관련 키
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_MODEL_SERVER_URL = "model_server_url"

    // [추가] 앱 버전 관리 키
    private const val KEY_APP_VERSION = "app_version_cache"

    private lateinit var prefs: SharedPreferences

    /**
     * Initializes the TokenManager. This must be called once, typically in the Application class.
     * @param context The application context.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the authentication token.
     * @param token The token to save.
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Retrieves the authentication token.
     * @return The saved token, or null if it doesn't exist.
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Saves the user's ID.
     */
    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * Retrieves the user's ID.
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Saves the user's name.
     */
    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * Retrieves the user's name.
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Saves the user's email.
     */
    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Retrieves the user's email.
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
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

    /**
     * Clears the authentication token and user data, effectively logging the user out.
     */
    fun clearToken() {
        prefs.edit().clear().apply()
    }

    // [추가] 저장된 캐시 버전(앱 버전) 가져오기
    fun getSavedAppVersion(): String? {
        return prefs.getString(KEY_APP_VERSION, null)
    }

    // [추가] 현재 앱 버전을 캐시에 저장
    fun saveAppVersion(version: String) {
        prefs.edit().putString(KEY_APP_VERSION, version).apply()
    }

    // [추가] 모든 캐시 데이터 삭제 (로그아웃/초기화 시 사용)
    // 앱 버전 정보는 지우지 않거나, 초기화 후 바로 다시 저장해야 합니다.
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
