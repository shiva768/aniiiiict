package com.zelretch.aniiiiiict.ui.base

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.IOException

class ErrorHandlerTest : BehaviorSpec({

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

        `when`("不明な例外を解析する場合") {
            then("不明エラーとして分類される") {
                val exception = RuntimeException("Unknown error")
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.UNKNOWN
                errorInfo.message shouldBe "Unknown error"
                errorInfo.originalException shouldBe exception
            }
        }

        `when`("メッセージがnullの例外を解析する場合") {
            then("デフォルトメッセージが使用される") {
                val exception = IOException(null as String?)
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.NETWORK
                errorInfo.message shouldBe "ネットワークエラーが発生しました"
            }
        }

        `when`("ユーザー向けメッセージを取得する場合") {
            then("エラータイプに応じた適切なメッセージが返される") {
                val networkError = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.NETWORK,
                    "Connection failed",
                    IOException()
                )
                val unknownError = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.UNKNOWN,
                    "Unknown",
                    RuntimeException()
                )

                ErrorHandler.getUserMessage(networkError) shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.getUserMessage(unknownError) shouldBe "処理中にエラーが発生しました"
            }
        }

        `when`("handleErrorメソッドを使用する場合") {
            then("適切なエラーメッセージが返される") {
                val ioException = IOException("Network error")
                val runtimeException = RuntimeException("Unknown error")

                ErrorHandler.handleError(ioException, "TestClass", "testMethod") shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.handleError(runtimeException, "TestClass", "testMethod") shouldBe "処理中にエラーが発生しました"
            }
        }
    }
})
