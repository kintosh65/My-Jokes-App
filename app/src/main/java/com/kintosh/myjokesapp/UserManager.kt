package com.kintosh.myjokesapp

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_LOGGED_IN_USER = "logged_in_user"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun signUp(context: Context, username: String, password: String): Boolean {
        val prefs = getPrefs(context)
        if (prefs.contains(username)) return false // User already exists
        
        prefs.edit().putString(username, password).apply()
        return true
    }

    fun login(context: Context, username: String, password: String): Boolean {
        val prefs = getPrefs(context)
        val savedPassword = prefs.getString(username, null)
        if (savedPassword == password) {
            prefs.edit().putString(KEY_LOGGED_IN_USER, username).apply()
            return true
        }
        return false
    }

    fun updateProfile(context: Context, newUsername: String, newPassword: String): Boolean {
        val prefs = getPrefs(context)
        val currentUsername = getLoggedInUser(context) ?: return false
        val currentPassword = prefs.getString(currentUsername, "") ?: ""

        // If username changes, we need to migrate the password and delete old entry
        if (newUsername != currentUsername) {
            if (prefs.contains(newUsername)) return false // Target username exists
            prefs.edit()
                .remove(currentUsername)
                .putString(newUsername, newPassword)
                .putString(KEY_LOGGED_IN_USER, newUsername)
                .apply()
        } else {
            // Just update password
            prefs.edit().putString(currentUsername, newPassword).apply()
        }
        return true
    }

    fun getLoggedInUser(context: Context): String? {
        return getPrefs(context).getString(KEY_LOGGED_IN_USER, null)
    }

    fun logout(context: Context) {
        getPrefs(context).edit().remove(KEY_LOGGED_IN_USER).apply()
    }
}
