package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import androidx.core.content.edit
import com.zelretch.aniiiiiict.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    context: Context,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "annict_prefs"
        private const val TOKEN_KEY = "access_token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            logger.logError(
                TAG,
                "空のトークンを保存しようとしました",
                "saveAccessToken"
            )
            return
        }

        logger.logInfo(
            TAG,
            "アクセストークンを保存: ${token.take(10)}...",
            "saveAccessToken"
        )

        try {
            prefs.edit { putString(TOKEN_KEY, token) }
        } catch (e: Exception) {
            logger.logError(TAG, e, "アクセストークンの保存に失敗")
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        if (token == null) {
            logger.logDebug(
                TAG,
                "保存されたアクセストークンがありません",
                "getAccessToken"
            )
        } else {
            logger.logInfo(
                TAG,
                "アクセストークンを取得: ${token.take(10)}...",
                "getAccessToken"
            )
        }
        return token
    }

    fun clearAccessToken() {
        try {
            prefs.edit { remove(TOKEN_KEY) }
            logger.logInfo(TAG, "アクセストークンを削除", "clearAccessToken")
        } catch (e: Exception) {
            logger.logError(TAG, e, "アクセストークンの削除に失敗")
        }
    }

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        logger.logDebug(TAG, "有効なトークンがあるか: $hasToken", "hasValidToken")
        return hasToken
    }
}
