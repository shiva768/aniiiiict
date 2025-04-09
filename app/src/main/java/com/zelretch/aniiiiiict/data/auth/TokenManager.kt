package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import android.util.Log
import com.zelretch.aniiiiiict.util.ErrorLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "annict_prefs"
        private const val TOKEN_KEY = "access_token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            Log.e(TAG, "空のトークンを保存しようとしました")
            ErrorLogger.logWarning("空のトークンを保存しようとしました", "saveAccessToken")
            return
        }

        Log.d(TAG, "アクセストークンを保存: ${token.take(10)}...")
        ErrorLogger.logInfo("アクセストークンを保存: ${token.take(10)}...", "saveAccessToken")

        try {
            prefs.edit().putString(TOKEN_KEY, token).apply()
            Log.d(TAG, "アクセストークンの保存に成功")
        } catch (e: Exception) {
            Log.e(TAG, "アクセストークンの保存に失敗: ${e.message}", e)
            ErrorLogger.logError(e, "アクセストークンの保存に失敗")
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        if (token == null) {
            Log.d(TAG, "保存されたアクセストークンがありません")
        } else {
            Log.d(TAG, "アクセストークンを取得: ${token.take(10)}...")
        }
        return token
    }

    fun clearToken() {
        Log.d(TAG, "アクセストークンをクリア")
        ErrorLogger.logInfo("アクセストークンをクリア", "clearToken")
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        Log.d(TAG, "有効なトークンがあるか: $hasToken")
        return hasToken
    }
}
