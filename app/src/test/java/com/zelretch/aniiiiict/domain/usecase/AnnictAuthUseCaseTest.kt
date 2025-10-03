package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AnnictAuthUseCase")
class AnnictAuthUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: AnnictAuthUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = AnnictAuthUseCase(repository)
    }

    @Nested
    @DisplayName("認証状態の確認")
    inner class IsAuthenticated {

        @Test
        @DisplayName("Repository認証済みの場合trueを返す")
        fun Repository認証済みの場合trueを返す() = kotlinx.coroutines.test.runTest {
            // Given
            coEvery { repository.isAuthenticated() } returns true

            // When
            val result = useCase.isAuthenticated()

            // Then
            assertEquals(true, result)
        }

        @Test
        @DisplayName("Repository未認証の場合falseを返す")
        fun Repository未認証の場合falseを返す() = kotlinx.coroutines.test.runTest {
            // Given
            coEvery { repository.isAuthenticated() } returns false

            // When
            val result = useCase.isAuthenticated()

            // Then
            assertEquals(false, result)
        }
    }

    @Nested
    @DisplayName("認証URL取得")
    inner class GetAuthUrl {

        @Test
        @DisplayName("正しいURLが返される")
        fun 正しいURLが返される() = kotlinx.coroutines.test.runTest {
            // Given
            val authUrl = "https://example.com/auth"
            coEvery { repository.getAuthUrl() } returns authUrl

            // When
            val result = useCase.getAuthUrl()

            // Then
            assertEquals(authUrl, result)
        }
    }

    @Nested
    @DisplayName("認証コールバック処理")
    inner class HandleAuthCallback {

        @Test
        @DisplayName("有効なコードで成功する")
        fun 有効なコードで成功する() = kotlinx.coroutines.test.runTest {
            // Given
            val code = "valid_code"
            coEvery { repository.handleAuthCallback(code) } returns true

            // When
            val result = useCase.handleAuthCallback(code)

            // Then
            assertEquals(true, result)
        }

        @Test
        @DisplayName("nullコードで失敗する")
        fun nullコードで失敗する() = kotlinx.coroutines.test.runTest {
            // When
            val result = useCase.handleAuthCallback(null)

            // Then
            assertEquals(false, result)
        }

        @Test
        @DisplayName("無効なコードで失敗する")
        fun 無効なコードで失敗する() = kotlinx.coroutines.test.runTest {
            // Given
            val code = "invalid_code"
            coEvery { repository.handleAuthCallback(code) } returns false

            // When
            val result = useCase.handleAuthCallback(code)

            // Then
            assertEquals(false, result)
        }
    }
}
