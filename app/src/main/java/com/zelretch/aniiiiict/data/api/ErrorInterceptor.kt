package com.zelretch.aniiiiict.data.api

import com.zelretch.aniiiiict.data.exception.NetworkException
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

    private companion object {
        private const val PEEK_BODY_BYTES: Int = 8 * 1024
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_RATE_LIMIT = 429
        private const val HTTP_SERVER_ERROR_MIN = 500
        private const val HTTP_SERVER_ERROR_MAX = 599
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        try {
            val response = chain.proceed(request)

            // HTTP ステータスの判定
            if (!response.isSuccessful) {
                val code = response.code
                // peekBody は OkHttp 4.9+ で利用可能。大きすぎる場合は先頭だけ。
                val bodyString = response.peekBody(PEEK_BODY_BYTES.toLong()).string()

                val message = "HTTP $code for $url"
                response.close()
                throw when (code) {
                    HTTP_UNAUTHORIZED -> NetworkException.UnauthorizedException(code, url, message, body = bodyString)
                    HTTP_FORBIDDEN -> NetworkException.ForbiddenException(code, url, message, body = bodyString)
                    HTTP_NOT_FOUND -> NetworkException.NotFoundException(code, url, message, body = bodyString)
                    HTTP_RATE_LIMIT -> NetworkException.RateLimitException(code, url, message, body = bodyString)
                    in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> NetworkException.ServerErrorException(
                        code,
                        url,
                        message,
                        body = bodyString
                    )

                    else -> NetworkException.UnknownHttpException(code, url, message, body = bodyString)
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
