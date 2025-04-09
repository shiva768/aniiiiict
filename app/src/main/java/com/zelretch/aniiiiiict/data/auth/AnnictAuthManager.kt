package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.AnnictConfig
import com.zelretch.aniiiiiict.data.model.TokenResponse
import com.zelretch.aniiiiiict.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class AnnictAuthManager @Inject constructor(
    private val context: Context,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val REDIRECT_URI = "aniiiiiict://oauth/callback"
        private const val TAG = "AnnictAuthManager"
    }

    fun getAuthorizationUrl(): String {
        return "${AnnictConfig.AUTH_URL}?" +
                "client_id=${BuildConfig.ANNICT_CLIENT_ID}&" +
                "response_type=code&" +
                "redirect_uri=$REDIRECT_URI&" +
                "scope=read+write"
    }

    suspend fun handleAuthorizationCode(code: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "認証コードの処理を開始: ${code.take(5)}...")
                ErrorLogger.logInfo(
                    "認証コードの処理を開始: ${code.take(5)}...",
                    "handleAuthorizationCode"
                )
                val tokenResponse = getAccessToken(code)
                Log.d(TAG, "アクセストークンを取得: ${tokenResponse.accessToken.take(10)}...")
                ErrorLogger.logInfo(
                    "アクセストークンを取得: ${tokenResponse.accessToken.take(10)}...",
                    "handleAuthorizationCode"
                )

                tokenManager.saveAccessToken(tokenResponse.accessToken)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "認証処理中にエラーが発生: ${e.message}", e)
                ErrorLogger.logError(e, "認証コードの処理中にエラー")
                Result.failure(e)
            }
        }
    }

    private suspend fun getAccessToken(code: String): TokenResponse {
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("client_id", BuildConfig.ANNICT_CLIENT_ID)
            .add("client_secret", BuildConfig.ANNICT_CLIENT_SECRET)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", REDIRECT_URI)
            .add("code", code)
            .build()

        Log.d(
            TAG,
            "トークンリクエストを送信: client_id=${BuildConfig.ANNICT_CLIENT_ID}, redirect_uri=$REDIRECT_URI"
        )
        ErrorLogger.logInfo("トークンリクエストを送信", "getAccessToken")

        val request = Request.Builder()
            .url(AnnictConfig.TOKEN_URL)
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "レスポンスボディなし"
            Log.e(TAG, "トークンリクエストが失敗: ${response.code}, body: $errorBody")
            ErrorLogger.logError(
                IOException("トークンリクエスト失敗: ${response.code}, $errorBody"),
                "getAccessToken"
            )
            throw IOException("Token request failed: ${response.code}, $errorBody")
        }

        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            Log.e(TAG, "レスポンスボディが空")
            ErrorLogger.logError(IOException("空のレスポンスボディ"), "getAccessToken")
            throw IOException("Empty response body")
        }

        Log.d(TAG, "トークンレスポンス受信: ${responseBody.take(50)}...")
        ErrorLogger.logInfo("トークンレスポンス受信", "getAccessToken")

        return try {
            Gson().fromJson(responseBody, TokenResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "JSONパースエラー: ${e.message}, body: $responseBody", e)
            ErrorLogger.logError(e, "トークンレスポンスのパース失敗")
            throw IOException("Failed to parse token response: ${e.message}")
        }
    }
}
