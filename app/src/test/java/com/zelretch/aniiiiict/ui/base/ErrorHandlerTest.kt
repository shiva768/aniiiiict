package com.zelretch.aniiiiict.ui.base

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.IOException

class エラーハンドラーテスト : BehaviorSpec({

    given("エラーハンドラー") {

        `when`("IOExceptionを解析する場合") {
            then("ネットワークエラーとして分類される") {
                val exception = IOException("Network connection failed")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.NETWORK
                errorInfo.message shouldBe "Network connection failed"
                errorInfo.originalException shouldBe exception
            }
        }

        `when`("タイムアウトエラーを解析する場合") {
            then("適切なユーザーメッセージが設定される") {
                val exception = IOException("Connection timeout")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.NETWORK
                errorInfo.userMessage shouldBe "接続がタイムアウトしました。ネットワーク接続を確認してください"
            }
        }

        `when`("API系エラーメッセージの処理を検証する場合") {
            then("適切なAPIエラーメッセージが返される") {
                // 401エラーは認証エラーとして処理される
                val authException = RuntimeException("401 Unauthorized")
                val authError = ErrorHandler.analyzeError(authException)
                authError.type shouldBe ErrorHandler.ErrorType.AUTH
                authError.userMessage shouldBe "認証に失敗しました。再度ログインしてください"

                // handleErrorメソッドでも確認
                ErrorHandler.handleError(authException, "TestClass") shouldBe "認証に失敗しました。再度ログインしてください"
            }
        }

        `when`("認証関連エラーを解析する場合") {
            then("認証エラーとして分類される") {
                val exception = RuntimeException("Token expired")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.AUTH
                errorInfo.userMessage shouldBe "認証に失敗しました。再度ログインしてください"
            }
        }

        `when`("記録作成失敗エラーを解析する場合") {
            then("ビジネスロジックエラーとして分類される") {
                val exception = Exception("Record creation failed")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.BUSINESS
                errorInfo.userMessage shouldBe "エピソードの記録に失敗しました。しばらく時間をおいてからお試しください"
            }
        }

        `when`("TokenManagerのコンテキストでエラーを解析する場合") {
            then("認証エラーとして分類される") {
                val exception = RuntimeException("Save failed")
                val errorInfo = ErrorHandler.analyzeError(exception, "TokenManager.saveAccessToken")

                errorInfo.type shouldBe ErrorHandler.ErrorType.AUTH
                errorInfo.userMessage shouldBe "認証情報の保存に失敗しました。アプリを再起動してください"
            }
        }

        `when`("不明な例外を解析する場合") {
            then("不明エラーとして分類される") {
                val exception = RuntimeException("Unknown error")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.UNKNOWN
                errorInfo.message shouldBe "Unknown error"
                errorInfo.originalException shouldBe exception
            }
        }

        `when`("ユーザー向けメッセージを取得する場合") {
            then("カスタムメッセージまたはデフォルトメッセージが返される") {
                val networkErrorWithCustom = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.NETWORK,
                    "Connection failed",
                    IOException(),
                    "カスタムネットワークエラー"
                )
                val networkErrorWithoutCustom = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.NETWORK,
                    "Connection failed",
                    IOException()
                )
                val authError = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.AUTH,
                    "Auth failed",
                    RuntimeException()
                )

                ErrorHandler.getUserMessage(networkErrorWithCustom) shouldBe "カスタムネットワークエラー"
                ErrorHandler.getUserMessage(networkErrorWithoutCustom) shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.getUserMessage(authError) shouldBe "認証に失敗しました。再度ログインしてください"
            }
        }

        `when`("handleErrorメソッドを使用する場合") {
            then("適切なエラーメッセージが返される") {
                val ioException = IOException("Network error")
                val authException = RuntimeException("Token error")
                val businessException = Exception("Record creation failed")

                ErrorHandler.handleError(
                    ioException,
                    "TestClass",
                    "testMethod"
                ) shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.handleError(
                    authException,
                    "TestClass",
                    "testMethod"
                ) shouldBe "認証に失敗しました。再度ログインしてください"
                ErrorHandler.handleError(
                    businessException,
                    "TestClass",
                    "testMethod"
                ) shouldBe "エピソードの記録に失敗しました。しばらく時間をおいてからお試しください"
            }
        }
    }
})
