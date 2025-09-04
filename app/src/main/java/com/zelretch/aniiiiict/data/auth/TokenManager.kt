package com.zelretch.aniiiiict.data.auth

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @VisibleForTesting internal constructor(
    private val prefs: android.content.SharedPreferences
) {
    companion object {
        private const val PREFS_NAME = "annict_prefs"
        private const val TOKEN_KEY = "access_token"
        private const val TOKEN_LOG_LENGTH = 10
    }

    @Inject
    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            Timber.e("[TokenManager][saveAccessToken] 空のトークンを保存しようとしました")
            return
        }
        Timber.i("[TokenManager][saveAccessToken] アクセストークンを保存: ${token.take(TOKEN_LOG_LENGTH)}...")
        prefs.edit { putString(TOKEN_KEY, token) }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        if (token == null) {
            Timber.d("[TokenManager][getAccessToken] 保存されたアクセストークンがありません")
        } else {
            Timber.i("[TokenManager][getAccessToken] アクセストークンを取得: ${token.take(TOKEN_LOG_LENGTH)}...")
        }
        return token
    }

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        Timber.d("[TokenManager][hasValidToken] 有効なトークンがあるか: $hasToken")
        return hasToken
    }
}
