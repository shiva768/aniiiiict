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
        fun Repository成功時にResultSuccessを返す() = runTest {
            // Given
            coEvery { repository.updateWorkViewStatus(any(), any()) } returns true

            // When
            val result = useCase("w1", StatusState.WATCHING)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("Repository失敗時でもResult.successを返す（警告ログ出力）")
        fun Repository失敗時でもResultSuccessを返す() = runTest {
            // Given
            coEvery { repository.updateWorkViewStatus(any(), any()) } returns false

            // When
            val result = useCase("w1", StatusState.WATCHING)

            // Then
            assertTrue(result.isSuccess)
        }
    }
}
