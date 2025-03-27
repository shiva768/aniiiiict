package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("annict_prefs", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        Log.d("TokenManager", "Access Token: $token")
        prefs.edit().putString("access_token", token).apply()
    }

    fun getAccessToken(): String? {
        val token = prefs.getString("access_token", null)
        Log.d("TokenManager", "Retrieved Access Token: $token")
        return token
    }

    fun clearToken() {
        Log.d("TokenManager", "Clearing Access Token")
        prefs.edit().remove("access_token").apply()
    }

    fun hasValidToken(): Boolean {
        val hasToken = !getAccessToken().isNullOrEmpty()
        Log.d("TokenManager", "Has Valid Token: $hasToken")
        return hasToken
    }
}
