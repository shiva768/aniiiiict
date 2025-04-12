package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    context: Context
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
            AniiiiiictLogger.logWarning(
                TAG,
                "空のトークンを保存しようとしました",
                "saveAccessToken"
            )
            return
        }

        AniiiiiictLogger.logInfo(
            TAG,
            "アクセストークンを保存: ${token.take(10)}...",
            "saveAccessToken"
        )

        try {
            prefs.edit { putString(TOKEN_KEY, token) }
        } catch (e: Exception) {
            AniiiiiictLogger.logError(TAG, e, "アクセストークンの保存に失敗")
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

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        Log.d(TAG, "有効なトークンがあるか: $hasToken")
        return hasToken
    }
}
