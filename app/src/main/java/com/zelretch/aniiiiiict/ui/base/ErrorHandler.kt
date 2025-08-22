package com.zelretch.aniiiiiict.ui.base

import com.apollographql.apollo.exception.ApolloException
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
        UNKNOWN // 不明なエラー
    }

    /**
     * エラー情報
     */
    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val originalException: Throwable
    )

    /**
     * 例外を解析してエラー情報を生成する
     */
    fun analyzeError(exception: Throwable): ErrorInfo = when (exception) {
        is IOException -> ErrorInfo(
            type = ErrorType.NETWORK,
            message = exception.message ?: "ネットワークエラーが発生しました",
            originalException = exception
        )
        is ApolloException -> ErrorInfo(
            type = ErrorType.API,
            message = exception.message ?: "APIエラーが発生しました",
            originalException = exception
        )
        else -> ErrorInfo(
            type = ErrorType.UNKNOWN,
            message = exception.message ?: "予期しないエラーが発生しました",
            originalException = exception
        )
    }

    /**
     * エラーをログに出力する
     */
    fun logError(className: String, methodName: String?, errorInfo: ErrorInfo) {
        val context = if (methodName != null) "[$className][$methodName]" else "[$className]"
        when (errorInfo.type) {
            ErrorType.NETWORK -> {
                Timber.e(errorInfo.originalException, "%s ネットワークエラーが発生: %s", context, errorInfo.message)
            }
            ErrorType.API -> {
                Timber.e(errorInfo.originalException, "%s APIエラーが発生: %s", context, errorInfo.message)
            }
            ErrorType.UNKNOWN -> {
                Timber.e(errorInfo.originalException, "%s 予期しないエラーが発生: %s", context, errorInfo.message)
            }
        }
    }

    /**
     * ユーザー向けエラーメッセージを取得する
     */
    fun getUserMessage(errorInfo: ErrorInfo): String = when (errorInfo.type) {
        ErrorType.NETWORK -> "ネットワーク接続を確認してください"
        ErrorType.API -> "サーバーとの通信に失敗しました"
        ErrorType.UNKNOWN -> "処理中にエラーが発生しました"
    }

    /**
     * エラーを処理し、ユーザー向けメッセージを返す
     */
    fun handleError(
        exception: Throwable,
        className: String,
        methodName: String? = null
    ): String {
        val errorInfo = analyzeError(exception)
        logError(className, methodName, errorInfo)
        return getUserMessage(errorInfo)
    }
}