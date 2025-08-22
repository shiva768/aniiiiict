package com.zelretch.aniiiiiict.ui.base

import com.apollographql.apollo.exception.ApolloException
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

        `when`("ApolloExceptionを解析する場合") {
            then("APIエラーとして分類される") {
                // ApolloExceptionは抽象クラスなので、テスト用にサブクラスを作成
                val exception = object : ApolloException("GraphQL error") {}
                val errorInfo = ErrorHandler.analyzeError(exception)

                errorInfo.type shouldBe ErrorHandler.ErrorType.API
                errorInfo.message shouldBe "GraphQL error"
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
                val apiError = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.API,
                    "GraphQL error",
                    object : ApolloException("test") {}
                )
                val unknownError = ErrorHandler.ErrorInfo(
                    ErrorHandler.ErrorType.UNKNOWN,
                    "Unknown",
                    RuntimeException()
                )

                ErrorHandler.getUserMessage(networkError) shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.getUserMessage(apiError) shouldBe "サーバーとの通信に失敗しました"
                ErrorHandler.getUserMessage(unknownError) shouldBe "処理中にエラーが発生しました"
            }
        }

        `when`("handleErrorメソッドを使用する場合") {
            then("適切なエラーメッセージが返される") {
                val ioException = IOException("Network error")
                val apolloException = object : ApolloException("API error") {}
                val runtimeException = RuntimeException("Unknown error")

                ErrorHandler.handleError(ioException, "TestClass", "testMethod") shouldBe "ネットワーク接続を確認してください"
                ErrorHandler.handleError(apolloException, "TestClass", "testMethod") shouldBe "サーバーとの通信に失敗しました"
                ErrorHandler.handleError(runtimeException, "TestClass", "testMethod") shouldBe "処理中にエラーが発生しました"
            }
        }
    }
})