package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DeleteRecordUseCase")
class DeleteRecordUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: DeleteRecordUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = DeleteRecordUseCase(repository)
    }

    @Nested
    @DisplayName("レコード削除")
    inner class DeleteRecord {

        @Test
        @DisplayName("成功時にResult.successを返す")
        fun onSuccess() = runTest {
            // Given
            coEvery { repository.deleteRecord("record1") } returns Result.success(Unit)

            // When
            val result = useCase("record1")

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("失敗時にResult.failureを返す")
        fun onFailure() = runTest {
            // Given
            coEvery { repository.deleteRecord("record2") } returns Result.failure(RuntimeException("error"))

            // When
            val result = useCase("record2")

            // Then
            assertTrue(result.isFailure)
        }
    }
}
