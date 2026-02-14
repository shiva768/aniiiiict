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

@DisplayName("WatchEpisodeUseCase")
class WatchEpisodeUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var useCase: WatchEpisodeUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        updateViewStateUseCase = mockk()
        useCase = WatchEpisodeUseCase(repository, updateViewStateUseCase)
    }

    @Nested
    @DisplayName("エピソード視聴記録")
    inner class RecordEpisode {

        @Test
        @DisplayName("記録成功時にResult.successを返す")
        fun onSuccess() = runTest {
            // Given
            coEvery { repository.createRecord(any(), any()) } returns Result.success(Unit)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)

            // When
            val result = useCase("ep1", "w1", StatusState.WANNA_WATCH, true)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("記録失敗時にResult.failureを返す")
        fun onFailure() = runTest {
            // Given
            coEvery { repository.createRecord(any(), any()) } returns Result.failure(RuntimeException("error"))

            // When
            val result = useCase("ep1", "w1", StatusState.WANNA_WATCH, true)

            // Then
            assertTrue(result.isFailure)
        }
    }
}
