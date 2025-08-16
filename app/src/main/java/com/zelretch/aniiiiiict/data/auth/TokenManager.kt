import timber.log.Timber
package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import androidx.core.content.edit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(context: Context) {
    companion object {
        private const val PREFS_NAME = "annict_prefs"
        private const val TOKEN_KEY = "access_token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            Timber.e("[TokenManager][saveAccessToken] 空のトークンを保存しようとしました")
            return
        }

        Timber.i("[TokenManager][saveAccessToken] アクセストークンを保存: ${token.take(10)}...")

        try {
            prefs.edit { putString(TOKEN_KEY, token) }
        } catch (e: Exception) {
            Timber.e(e, "[TokenManager][saveAccessToken] アクセストークンの保存に失敗")
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        if (token == null) {
            Timber.d("[TokenManager][getAccessToken] 保存されたアクセストークンがありません")
        } else {
            Timber.i("[TokenManager][getAccessToken] アクセストークンを取得: ${token.take(10)}...")
        }
        return token
    }

    fun clearAccessToken() {
        try {
            prefs.edit { remove(TOKEN_KEY) }
            Timber.i("[TokenManager][clearAccessToken] アクセストークンを削除")
        } catch (e: Exception) {
            Timber.e(e, "[TokenManager][clearAccessToken] アクセストークンの削除に失敗")
        }
    }

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val hasToken = !token.isNullOrEmpty()
        Timber.d("[TokenManager][hasValidToken] 有効なトークンがあるか: $hasToken")
        return hasToken
    }
}
