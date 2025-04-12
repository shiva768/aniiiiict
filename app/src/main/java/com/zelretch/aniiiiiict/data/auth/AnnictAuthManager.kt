package com.zelretch.aniiiiiict.data.auth

import android.util.Log
import com.google.gson.Gson
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.AnnictConfig
import com.zelretch.aniiiiiict.data.model.TokenResponse
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import com.zelretch.aniiiiiict.util.RetryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class AnnictAuthManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val retryManager: RetryManager
) {
    companion object {
        private const val REDIRECT_URI = "aniiiiiict://oauth/callback"
        private const val TAG = "AnnictAuthManager"
        private const val MAX_RETRIES = 3
    }

    fun getAuthorizationUrl(): String = buildString {
        append(AnnictConfig.AUTH_URL)
        append("?client_id=${BuildConfig.ANNICT_CLIENT_ID}")
        append("&response_type=code")
        append("&redirect_uri=$REDIRECT_URI")
        append("&scope=read+write")
    }

    suspend fun handleAuthorizationCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logInfo("認証コードの処理を開始: ${code.take(5)}...", "handleAuthorizationCode")
            val tokenResponse = getAccessTokenWithRetry(code)
            logInfo(
                "アクセストークンを取得: ${tokenResponse.accessToken.take(10)}...",
                "handleAuthorizationCode"
            )
            tokenManager.saveAccessToken(tokenResponse.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            logError(e, "認証コードの処理中にエラー")
            Result.failure(e)
        }
    }

    private suspend fun getAccessTokenWithRetry(code: String): TokenResponse =
        retryManager.retry(
            maxAttempts = MAX_RETRIES,
            initialDelay = 1000L,
            maxDelay = 5000L,
            factor = 2.0
        ) {
            getAccessToken(code)
        }

    private fun getAccessToken(code: String): TokenResponse {
        val formBody = FormBody.Builder()
            .add("client_id", BuildConfig.ANNICT_CLIENT_ID)
            .add("client_secret", BuildConfig.ANNICT_CLIENT_SECRET)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", REDIRECT_URI)
            .add("code", code)
            .build()

        logInfo("トークンリクエストを送信", "getAccessToken")

        val request = Request.Builder()
            .url(AnnictConfig.TOKEN_URL)
            .post(formBody)
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "レスポンスボディなし"
                val error = IOException("トークンリクエスト失敗: ${response.code}, $errorBody")
                logError(error, "getAccessToken")
                throw error
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                val error = IOException("空のレスポンスボディ")
                logError(error, "getAccessToken")
                throw error
            }

            logInfo("トークンレスポンス受信: ${responseBody.take(50)}...", "getAccessToken")

            try {
                Gson().fromJson(responseBody, TokenResponse::class.java)
            } catch (e: Exception) {
                logError(e, "トークンレスポンスのパース失敗")
                throw IOException("Failed to parse token response: ${e.message}")
            }
        }
    }

    private fun logInfo(message: String, method: String) {
        Log.d(TAG, message)
        AniiiiiictLogger.logInfo(message, method)
    }

    private fun logError(error: Exception, method: String) {
        Log.e(TAG, "${error.message}", error)
        AniiiiiictLogger.logError(error, method)
    }
}
