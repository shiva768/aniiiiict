package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import androidx.core.content.edit
import com.zelretch.aniiiiiict.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(context: Context, private val logger: Logger) {
    companion object {
        private const val PREFS_NAME = "annict_prefs"
        private const val TOKEN_KEY = "access_token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            logger.error(
                "TokenManager",
                "空のトークンを保存しようとしました",
                "saveAccessToken"
            )
            return
        }

        logger.info(
            "TokenManager",
            "アクセストークンを保存: ${token.take(10)}...",
            "saveAccessToken"
        )

        try {
            prefs.edit { putString(TOKEN_KEY, token) }
        } catch (e: Exception) {
            logger.error("TokenManager", e, "アクセストークンの保存に失敗")
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        if (token == null) {
            logger.debug(
                "TokenManager",
                "保存されたアクセストークンがありません",
                "getAccessToken"
            )
        } else {
            logger.info(
                "TokenManager",
                "アクセストークンを取得: ${token.take(10)}...",
                "getAccessToken"
            )
        }
        return token
    }

    fun clearAccessToken() {
        try {
            prefs.edit { remove(TOKEN_KEY) }
            logger.info("TokenManager", "アクセストークンを削除", "clearAccessToken")
        } catch (e: Exception) {
            logger.error("TokenManager", e, "アクセストークンの削除に失敗")
        }
    }

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        logger.debug("TokenManager", "有効なトークンがあるか: $hasToken", "hasValidToken")
        return hasToken
    }
}
