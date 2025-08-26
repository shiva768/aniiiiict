package com.zelretch.aniiiiiict.data.exception

/**
 * ネットワーク/HTTP エラーを表す共通の例外階層。
 * OkHttp の Interceptor（ErrorInterceptor）でスローされ、上位層で一元的に処理されることを想定。
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** ネットワーク未接続、ホスト解決不可など */
    class NoNetworkException(message: String, cause: Throwable? = null) : NetworkException(message, cause)

    /** タイムアウト（接続/読み取り/書き込み） */
    class TimeoutException(message: String, cause: Throwable? = null) : NetworkException(message, cause)

    /** SSL/TLS 証明書エラーなどセキュリティ関連 */
    class SecurityException(message: String, cause: Throwable? = null) : NetworkException(message, cause)

    /** HTTP ステータスコードに基づく例外（4xx/5xx など） */
    open class HttpException(
        val code: Int,
        val url: String?,
        message: String,
        cause: Throwable? = null,
        val body: String? = null
    ) : NetworkException(message, cause)

    /** 401 系の未認証 */
    class UnauthorizedException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)

    /** 403 禁止 */
    class ForbiddenException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)

    /** 404 未検出 */
    class NotFoundException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)

    /** 429 レート制限 */
    class RateLimitException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)

    /** 5xx サーバーエラー */
    class ServerErrorException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)

    /** 分類不能のその他 HTTP エラー */
    class UnknownHttpException(
        code: Int,
        url: String?,
        message: String,
        cause: Throwable? = null,
        body: String? = null
    ) : HttpException(code, url, message, cause, body)
}
