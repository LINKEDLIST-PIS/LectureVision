package com.son.lecture_project.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenManager {

    // --- Keys ---
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TICKET_INDICATOR = "ticket_indicator"
    private const val KEY_DEBUG_MODE = "debug_mode"
    private const val KEY_MODEL_SERVER_URL = "model_server_url"
    private const val KEY_APP_VERSION = "app_version_cache"
    private const val KEY_TOKEN_TIMESTAMP = "token_timestamp"
    private const val KEY_TICKET = "ticket"
    private const val KEY_TICKET_TIMESTAMP = "ticket_timestamp"
    private const val KEY_TICKET_USED = "ticket_used"

    // --- Validity Durations ---
    private const val TOKEN_VALIDITY_MS = 60 * 60 * 1000L // 1 hour
    private const val TICKET_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Time-based & Original Functions Combined ---

    fun saveToken(token: String) {
        prefs.edit {
            putString(KEY_TOKEN, token)
            putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun isTokenValid(): Boolean {
        val token = getToken()
        val timestamp = prefs.getLong(KEY_TOKEN_TIMESTAMP, 0)
        if (token.isNullOrEmpty() || timestamp == 0L) return false
        return (System.currentTimeMillis() - timestamp) < TOKEN_VALIDITY_MS
    }

    fun saveTicket(ticket: String) {
        prefs.edit {
            putString(KEY_TICKET, ticket)
            putLong(KEY_TICKET_TIMESTAMP, System.currentTimeMillis())
            putBoolean(KEY_TICKET_USED, false)
        }
    }

    fun getTicket(): String? {
        return prefs.getString(KEY_TICKET, null)
    }

    fun isTicketValid(): Boolean {
        val ticket = getTicket()
        if (ticket.isNullOrEmpty()) return false

        val timestamp = prefs.getLong(KEY_TICKET_TIMESTAMP, 0)
        val isUsed = prefs.getBoolean(KEY_TICKET_USED, true)

        if (isUsed || timestamp == 0L) return false

        return (System.currentTimeMillis() - timestamp) < TICKET_VALIDITY_MS
    }

    fun useTicket() {
        prefs.edit { putBoolean(KEY_TICKET_USED, true) }
    }

    fun saveUserId(userId: String) {
        prefs.edit { putString(KEY_USER_ID, userId) }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveUserName(name: String) {
        prefs.edit { putString(KEY_USER_NAME, name) }
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit { putString(KEY_USER_EMAIL, email) }
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    @Suppress("unused")
    fun saveUserPassword(password: String) {
        prefs.edit { putString(KEY_USER_PASSWORD, password) }
    }

    fun getUserPassword(): String? {
        return prefs.getString(KEY_USER_PASSWORD, null)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }
    
    fun setLanguage(langCode: String) {
        prefs.edit { putString(KEY_LANGUAGE, langCode) }
    }
    
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "ko") ?: "ko"
    }

    fun setTicketIndicatorVisible(visible: Boolean) {
        prefs.edit { putBoolean(KEY_TICKET_INDICATOR, visible) }
    }

    fun isTicketIndicatorVisible(): Boolean {
        return prefs.getBoolean(KEY_TICKET_INDICATOR, true)
    }
    
    fun setDebugMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DEBUG_MODE, enabled) }
    }
    
    fun isDebugMode(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_MODE, false)
    }
    
    fun setModelServerUrl(url: String) {
        prefs.edit { putString(KEY_MODEL_SERVER_URL, url) }
    }
    
    fun getModelServerUrl(): String? {
        return prefs.getString(KEY_MODEL_SERVER_URL, null)
    }

    fun clearToken() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_TOKEN_TIMESTAMP)
            putBoolean(KEY_IS_LOGGED_IN, false)
        }
    }

    fun getSavedAppVersion(): String? {
        return prefs.getString(KEY_APP_VERSION, null)
    }

    fun saveAppVersion(version: String) {
        prefs.edit { putString(KEY_APP_VERSION, version) }
    }

    fun clearAllData() {
        prefs.edit { 
            clear()
            putBoolean(KEY_IS_LOGGED_IN, false)
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    @Suppress("unused")
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }
}