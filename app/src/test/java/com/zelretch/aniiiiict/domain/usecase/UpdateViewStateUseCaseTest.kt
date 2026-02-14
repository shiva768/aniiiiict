package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UpdateViewStateUseCase")
class UpdateViewStateUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: UpdateViewStateUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = UpdateViewStateUseCase(repository)
    }

    @Nested
    @DisplayName("ステータス更新")
    inner class UpdateStatus {

        @Test
        @DisplayName("Repository成功時にResult.successを返す")
        fun onRepositorySuccess() = runTest {
            // Given
            coEvery { repository.updateWorkViewStatus(any(), any()) } returns Result.success(Unit)

            // When
            val result = useCase("w1", StatusState.WATCHING)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("Repository失敗時にResult.failureを返す")
        fun onRepositoryFailure() = runTest {
            // Given
            coEvery { repository.updateWorkViewStatus(any(), any()) } returns Result.failure(RuntimeException("error"))

            // When
            val result = useCase("w1", StatusState.WATCHING)

            // Then
            assertTrue(result.isFailure)
        }
    }
}
