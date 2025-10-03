package com.zelretch.aniiiiict.ui.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException

@DisplayName("ErrorHandler")
class ErrorHandlerTest {

    @Nested
    @DisplayName("エラー解析")
    inner class AnalyzeError {

        @Test
        @DisplayName("IOExceptionはネットワークエラーとして分類される")
        fun IOExceptionはネットワークエラーとして分類される() {
            // Given
            val exception = IOException("Network connection failed")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception)

            // Then
            assertEquals(ErrorHandler.ErrorType.NETWORK, errorInfo.type)
            assertEquals("Network connection failed", errorInfo.message)
            assertEquals(exception, errorInfo.originalException)
        }

        @Test
        @DisplayName("タイムアウトエラーは適切なユーザーメッセージが設定される")
        fun タイムアウトエラーは適切なユーザーメッセージが設定される() {
            // Given
            val exception = IOException("Connection timeout")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception)

            // Then
            assertEquals(ErrorHandler.ErrorType.NETWORK, errorInfo.type)
            assertEquals("接続がタイムアウトしました。ネットワーク接続を確認してください", errorInfo.userMessage)
        }

        @Test
        @DisplayName("401エラーは認証エラーとして処理される")
        fun 認証エラーは認証エラーとして処理される() {
            // Given
            val authException = RuntimeException("401 Unauthorized")

            // When
            val authError = ErrorHandler.analyzeError(authException)

            // Then
            assertEquals(ErrorHandler.ErrorType.AUTH, authError.type)
            assertEquals("認証に失敗しました。再度ログインしてください", authError.userMessage)

            // handleErrorメソッドでも確認
            assertEquals("認証に失敗しました。再度ログインしてください", ErrorHandler.handleError(authException, "TestClass"))
        }

        @Test
        @DisplayName("認証関連エラーは認証エラーとして分類される")
        fun 認証関連エラーは認証エラーとして分類される() {
            // Given
            val exception = RuntimeException("Token expired")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception)

            // Then
            assertEquals(ErrorHandler.ErrorType.AUTH, errorInfo.type)
            assertEquals("認証に失敗しました。再度ログインしてください", errorInfo.userMessage)
        }

        @Test
        @DisplayName("記録作成失敗はビジネスロジックエラーとして分類される")
        fun 記録作成失敗はビジネスロジックエラーとして分類される() {
            // Given
            val exception = Exception("Record creation failed")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception)

            // Then
            assertEquals(ErrorHandler.ErrorType.BUSINESS, errorInfo.type)
            assertEquals("エピソードの記録に失敗しました。しばらく時間をおいてからお試しください", errorInfo.userMessage)
        }

        @Test
        @DisplayName("TokenManagerコンテキストは認証エラーとして分類される")
        fun TokenManagerコンテキストは認証エラーとして分類される() {
            // Given
            val exception = RuntimeException("Save failed")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception, "TokenManager.saveAccessToken")

            // Then
            assertEquals(ErrorHandler.ErrorType.AUTH, errorInfo.type)
            assertEquals("認証情報の保存に失敗しました。アプリを再起動してください", errorInfo.userMessage)
        }

        @Test
        @DisplayName("不明な例外はUNKNOWNエラーとして分類される")
        fun 不明な例外はUNKNOWNエラーとして分類される() {
            // Given
            val exception = RuntimeException("Unknown error")

            // When
            val errorInfo = ErrorHandler.analyzeError(exception)

            // Then
            assertEquals(ErrorHandler.ErrorType.UNKNOWN, errorInfo.type)
            assertEquals("Unknown error", errorInfo.message)
            assertEquals(exception, errorInfo.originalException)
        }
    }

    @Nested
    @DisplayName("ユーザーメッセージ取得")
    inner class GetUserMessage {

        @Test
        @DisplayName("カスタムメッセージがある場合それを返す")
        fun カスタムメッセージがある場合それを返す() {
            // Given
            val networkErrorWithCustom = ErrorHandler.ErrorInfo(
                ErrorHandler.ErrorType.NETWORK,
                "Connection failed",
                IOException(),
                "カスタムネットワークエラー"
            )

            // When
            val result = ErrorHandler.getUserMessage(networkErrorWithCustom)

            // Then
            assertEquals("カスタムネットワークエラー", result)
        }

        @Test
        @DisplayName("カスタムメッセージがない場合デフォルトメッセージを返す")
        fun カスタムメッセージがない場合デフォルトメッセージを返す() {
            // Given
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

            // When & Then
            assertEquals("ネットワーク接続を確認してください", ErrorHandler.getUserMessage(networkErrorWithoutCustom))
            assertEquals("認証に失敗しました。再度ログインしてください", ErrorHandler.getUserMessage(authError))
        }
    }

    @Nested
    @DisplayName("handleErrorメソッド")
    inner class HandleError {

        @Test
        @DisplayName("各種エラーで適切なメッセージが返される")
        fun 各種エラーで適切なメッセージが返される() {
            // Given
            val ioException = IOException("Network error")
            val authException = RuntimeException("Token error")
            val businessException = Exception("Record creation failed")

            // When & Then
            assertEquals(
                "ネットワーク接続を確認してください",
                ErrorHandler.handleError(ioException, "TestClass", "testMethod")
            )
            assertEquals(
                "認証に失敗しました。再度ログインしてください",
                ErrorHandler.handleError(authException, "TestClass", "testMethod")
            )
            assertEquals(
                "エピソードの記録に失敗しました。しばらく時間をおいてからお試しください",
                ErrorHandler.handleError(businessException, "TestClass", "testMethod")
            )
        }
    }
}
