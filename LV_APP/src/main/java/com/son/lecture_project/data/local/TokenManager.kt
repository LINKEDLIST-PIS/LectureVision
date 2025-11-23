package com.son.lecture_project.data.local

import android.content.Context
import android.content.SharedPreferences


object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    
    // 설정 관련 키 추가
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_LANGUAGE = "language"

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
        // 기본값: false (시스템 설정을 따를 수도 있지만 여기선 false)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true) // 기본값: 켜짐
    }
    
    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
    }
    
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "ko") ?: "ko" // 기본값: 한국어
    }

    /**
     * Clears the authentication token and user data, effectively logging the user out.
     */
    fun clearToken() {
        prefs.edit().clear().apply()
    }
}
