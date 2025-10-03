package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
        @DisplayName("成功時にtrueを返す")
        fun 成功時にtrueを返す() = runTest {
            // Given
            coEvery { repository.deleteRecord("record1") } returns true

            // When
            val result = useCase("record1")

            // Then
            assertEquals(true, result)
        }

        @Test
        @DisplayName("失敗時にfalseを返す")
        fun 失敗時にfalseを返す() = runTest {
            // Given
            coEvery { repository.deleteRecord("record2") } returns false

            // When
            val result = useCase("record2")

            // Then
            assertEquals(false, result)
        }
    }
}
