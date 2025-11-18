package com.son.lecture_project.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * A singleton object to manage the user's authentication token using SharedPreferences.
 * This manager handles saving, retrieving, and clearing the token.
 */
object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"

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
     * Clears the authentication token, effectively logging the user out.
     */
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
