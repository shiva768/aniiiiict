package com.zelretch.aniiiiiict.data.auth

import android.content.Context
import com.google.gson.Gson
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.api.AnnictConfig
import com.zelretch.aniiiiiict.data.model.TokenResponse
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
                val tokenResponse = getAccessToken(code)
                tokenManager.saveAccessToken(tokenResponse.accessToken)
                Result.success(Unit)
            } catch (e: Exception) {
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

        val request = Request.Builder()
            .url(AnnictConfig.TOKEN_URL)
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Token request failed: ${response.code}")
        }

        return Gson().fromJson(
            response.body?.string(),
            TokenResponse::class.java
        )
    }
}
