package com.zelretch.aniiiiiict.data.auth

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.AnnictConfig
import com.zelretch.aniiiiiict.data.model.TokenResponse
import com.zelretch.aniiiiiict.util.RetryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class AnnictAuthManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val retryManager: RetryManager
) {
    companion object {
        private const val REDIRECT_URI = "aniiiiiict://oauth/callback"
        private const val MAX_RETRIES = 3
        private const val RETRY_INITIAL_DELAY_MS = 1000L
        private const val RETRY_MAX_DELAY_MS = 5000L
        private const val RETRY_FACTOR = 2.0
        private const val LOG_CODE_PREFIX_LENGTH = 5
        private const val LOG_TOKEN_PREFIX_LENGTH = 10
        private const val LOG_RESPONSE_PREFIX_LENGTH = 50
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
            Timber.i(
                "[AnnictAuthManager][handleAuthorizationCode] 認証コードの処理を開始: ${code.take(LOG_CODE_PREFIX_LENGTH)}..."
            )
            val tokenResponse = getAccessTokenWithRetry(code)
            Timber.i(
                "[AnnictAuthManager][handleAuthorizationCode] アクセストークンを取得: ${
                    tokenResponse.accessToken.take(
                        LOG_TOKEN_PREFIX_LENGTH
                    )
                }..."
            )
            tokenManager.saveAccessToken(tokenResponse.accessToken)
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(
                e,
                "[AnnictAuthManager][handleAuthorizationCode] %s",
                e.message ?: "Unknown error"
            )
            Result.failure(e)
        }
    }

    private suspend fun getAccessTokenWithRetry(code: String): TokenResponse {
        val retryConfig = com.zelretch.aniiiiiict.util.RetryConfig(
            maxAttempts = MAX_RETRIES,
            initialDelay = RETRY_INITIAL_DELAY_MS,
            maxDelay = RETRY_MAX_DELAY_MS,
            factor = RETRY_FACTOR
        )
        return retryManager.retry(config = retryConfig) {
            getAccessToken(code)
        }
    }

    private fun getAccessToken(code: String): TokenResponse {
        val formBody = FormBody.Builder()
            .add("client_id", BuildConfig.ANNICT_CLIENT_ID)
            .add("client_secret", BuildConfig.ANNICT_CLIENT_SECRET)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", REDIRECT_URI)
            .add("code", code)
            .build()

        Timber.i("[AnnictAuthManager][getAccessToken] トークンリクエストを送信")

        val request = Request.Builder().url(AnnictConfig.TOKEN_URL).post(formBody).build()

        return okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                val errorBody = responseBody ?: "レスポンスボディなし"
                val error = IOException("トークンリクエスト失敗: ${response.code}, $errorBody")
                Timber.e(error, "[AnnictAuthManager][getAccessToken] %s", error.message)
                throw error
            }

            Timber.i(
                "[AnnictAuthManager][getAccessToken] トークンレスポンス受信: ${
                    responseBody.take(
                        LOG_RESPONSE_PREFIX_LENGTH
                    )
                }..."
            )

            return runCatching {
                Gson().fromJson(responseBody, TokenResponse::class.java)
            }.getOrElse { e ->
                val error = IOException("トークンレスポンスのパースに失敗", e)
                Timber.e(error, "[AnnictAuthManager][getAccessToken] %s", e.message)
                throw error
            }
        }
    }
}
