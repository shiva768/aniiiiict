package com.zelretch.aniiiiiict.data.api

import com.zelretch.aniiiiiict.data.exception.NetworkException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * すべてのネットワーク通信を監視し、エラーを共通の例外にマッピングする Interceptor。
 * - ネットワーク未接続/タイムアウト/SSL エラーを NetworkException に変換
 * - HTTP ステータスコードに応じて NetworkException.HttpException の派生に変換
 */
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        try {
            val response = chain.proceed(request)

            // HTTP ステータスの判定
            if (!response.isSuccessful) {
                val code = response.code
                val bodyString = try {
                    // peekBody は OkHttp 4.9+ で利用可能。大きすぎる場合は先頭だけ。
                    response.peekBody(1024 * 8).string()
                } catch (e: Exception) {
                    throw e
                }
                val message = "HTTP $code for $url"
                response.close()
                throw when (code) {
                    401 -> NetworkException.UnauthorizedException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                    403 -> NetworkException.ForbiddenException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                    404 -> NetworkException.NotFoundException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                    429 -> NetworkException.RateLimitException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                    in 500..599 -> NetworkException.ServerErrorException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                    else -> NetworkException.UnknownHttpException(
                        code = code,
                        url = url,
                        message = message,
                        body = bodyString
                    )
                }
            }
            return response
        } catch (e: SocketTimeoutException) {
            throw NetworkException.TimeoutException("Timeout for $url", e)
        } catch (e: UnknownHostException) {
            throw NetworkException.NoNetworkException("No network for $url", e)
        } catch (e: SSLException) {
            throw NetworkException.SecurityException("SSL error for $url", e)
        } catch (e: EOFException) {
            // 一部環境での切断などをネットワークエラーとして扱う
            throw NetworkException.NoNetworkException("Connection closed for $url", e)
        } catch (e: IOException) {
            // その他の IO エラーは接続問題として扱う
            throw NetworkException.NoNetworkException("Network error for $url", e)
        }
    }
}
