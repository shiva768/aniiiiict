package com.zelretch.aniiiiict.ui.base

import com.apollographql.apollo.exception.ApolloException
import com.zelretch.aniiiiict.data.exception.NetworkException
import timber.log.Timber
import java.io.IOException

/**
 * エラー処理を統一するためのヘルパークラス
 */
object ErrorHandler {

    /**
     * エラータイプ
     */
    enum class ErrorType {
        NETWORK, // ネットワーク関連エラー
        API, // API関連エラー
        AUTH, // 認証関連エラー
        VALIDATION, // バリデーションエラー
        BUSINESS, // ビジネスロジックエラー
        UNKNOWN // 不明なエラー
    }

    /**
     * エラー情報
     */
    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val originalException: Throwable,
        val userMessage: String? = null
    )

    /**
     * 例外を解析してエラー情報を生成する
     */
    @Suppress("CyclomaticComplexMethod")
    fun analyzeError(exception: Throwable, context: String? = null): ErrorInfo {
        val message = exception.message ?: ""

        return when (exception) {
            is NetworkException -> when (exception) {
                is NetworkException.TimeoutException,
                is NetworkException.NoNetworkException,
                is NetworkException.SecurityException -> createNetworkErrorInfo(IOException(exception), message)
                is NetworkException.HttpException -> {
                    // HTTP 系は API エラーとして扱い、HTTP コードに応じたメッセージは既存ロジックに委譲
                    createErrorInfo(
                        type = ErrorType.API,
                        exception = exception,
                        message = exception.message ?: "",
                        userMessage = getApiErrorMessage(exception.message ?: "")
                    )
                }
            }
            is IOException -> createNetworkErrorInfo(exception, message)
            is ApolloException -> createApiErrorInfo(exception, message)
            // 認証エラーの判定を直接組み込み
            else -> when {
                message.contains("token", ignoreCase = true) ||
                    message.contains("auth", ignoreCase = true) ||
                    context?.contains("saveAccessToken") == true ||
                    context?.contains("TokenManager") == true ->
                    createAuthErrorInfo(exception, message, context)
                // ビジネスエラーの判定を直接組み込み
                message.contains("Record creation failed") ||
                    message.contains("retry", ignoreCase = true) ->
                    createBusinessErrorInfo(exception, message)
                else -> createErrorInfo(
                    type = ErrorType.UNKNOWN,
                    exception = exception,
                    message = message,
                    userMessage = "処理中にエラーが発生しました"
                )
            }
        }
    }

    private fun createNetworkErrorInfo(exception: IOException, message: String): ErrorInfo {
        val userMessage = when {
            message.contains("timeout", ignoreCase = true) ->
                "接続がタイムアウトしました。ネットワーク接続を確認してください"
            message.contains("connection", ignoreCase = true) ->
                "ネットワーク接続を確認してください"
            else -> "ネットワーク接続を確認してください"
        }
        return createErrorInfo(ErrorType.NETWORK, exception, message, userMessage)
    }

    private fun createApiErrorInfo(exception: ApolloException, message: String): ErrorInfo =
        createErrorInfo(ErrorType.API, exception, message, getApiErrorMessage(message))

    private fun createAuthErrorInfo(exception: Throwable, message: String, context: String?): ErrorInfo {
        val userMessage = when {
            message.contains("saveAccessToken") ||
                message.contains("TokenManager") ||
                context?.contains("saveAccessToken") == true ||
                context?.contains("TokenManager") == true ->
                "認証情報の保存に失敗しました。アプリを再起動してください"
            else -> "認証に失敗しました。再度ログインしてください"
        }
        return createErrorInfo(ErrorType.AUTH, exception, message, userMessage)
    }

    private fun createBusinessErrorInfo(exception: Throwable, message: String): ErrorInfo {
        val userMessage = when {
            message.contains("Record creation failed") ->
                "エピソードの記録に失敗しました。しばらく時間をおいてからお試しください"
            else -> "処理に失敗しました。しばらく時間をおいてからお試しください"
        }
        return createErrorInfo(ErrorType.BUSINESS, exception, message, userMessage)
    }

    private fun createErrorInfo(
        type: ErrorType,
        exception: Throwable,
        message: String,
        userMessage: String
    ): ErrorInfo = ErrorInfo(
        type = type,
        message = message,
        originalException = exception,
        userMessage = userMessage
    )

    private fun getApiErrorMessage(message: String): String = when {
        message.contains("401", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) ->
            "認証が必要です。再度ログインしてください"
        message.contains("403", ignoreCase = true) ||
            message.contains("forbidden", ignoreCase = true) ->
            "この操作を行う権限がありません"
        message.contains("404", ignoreCase = true) ||
            message.contains("not found", ignoreCase = true) ->
            "要求されたデータが見つかりません"
        message.contains("429", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ->
            "リクエストが多すぎます。しばらく時間をおいてからお試しください"
        message.contains("500", ignoreCase = true) ||
            message.contains("internal server", ignoreCase = true) ->
            "サーバーで問題が発生しています。しばらく時間をおいてからお試しください"
        message.contains("503", ignoreCase = true) ||
            message.contains("service unavailable", ignoreCase = true) ->
            "サービスが一時的に利用できません。しばらく時間をおいてからお試しください"
        else -> "サーバーとの通信に失敗しました"
    }

    /**
     * エラーをログに出力する
     */
    fun logError(className: String, methodName: String?, errorInfo: ErrorInfo) {
        val context = if (methodName != null) "[$className][$methodName]" else "[$className]"
        when (errorInfo.type) {
            ErrorType.NETWORK -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s ネットワークエラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
            ErrorType.API -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s APIエラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
            ErrorType.AUTH -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s 認証エラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
            ErrorType.VALIDATION -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s バリデーションエラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
            ErrorType.BUSINESS -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s ビジネスロジックエラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
            ErrorType.UNKNOWN -> {
                Timber.e(
                    errorInfo.originalException,
                    "%s 予期しないエラーが発生: %s",
                    context,
                    errorInfo.message
                )
            }
        }
    }

    /**
     * ユーザー向けエラーメッセージを取得する
     */
    fun getUserMessage(errorInfo: ErrorInfo): String = errorInfo.userMessage ?: when (errorInfo.type) {
        ErrorType.NETWORK -> "ネットワーク接続を確認してください"
        ErrorType.API -> "サーバーとの通信に失敗しました"
        ErrorType.AUTH -> "認証に失敗しました。再度ログインしてください"
        ErrorType.VALIDATION -> "入力内容に問題があります。確認してください"
        ErrorType.BUSINESS -> "処理に失敗しました。しばらく時間をおいてからお試しください"
        ErrorType.UNKNOWN -> "処理中にエラーが発生しました"
    }

    /**
     * エラーを処理し、ユーザー向けメッセージを返す
     */
    fun handleError(exception: Throwable, className: String, methodName: String? = null): String {
        val context = if (methodName != null) "$className.$methodName" else className
        val errorInfo = analyzeError(exception, context)
        logError(className, methodName, errorInfo)
        return getUserMessage(errorInfo)
    }
}
