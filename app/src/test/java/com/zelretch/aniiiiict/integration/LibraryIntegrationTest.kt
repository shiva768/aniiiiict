package com.zelretch.aniiiiict.integration

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration test to verify LoadLibraryEntriesUseCase works with AnnictRepository
 */
@DisplayName("ライブラリロード統合テスト")
class LibraryIntegrationTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: LoadLibraryEntriesUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = LoadLibraryEntriesUseCase(repository)
    }

    @Nested
    @DisplayName("ライブラリエントリーの取得")
    inner class LoadLibraryEntries {

        @Test
        @DisplayName("リポジトリからライブラリを正しく取得できる")
        fun loadsLibraryEntriesFromRepository() = runTest {
            // Given
            val entries = listOf(
                createLibraryEntry("entry1", "work1", "天国大魔境"),
                createLibraryEntry("entry2", "work2", "終物語"),
                createLibraryEntry("entry3", "work3", "SPY×FAMILY")
            )
            coEvery { repository.getLibraryEntries(listOf(StatusState.WATCHING), null) } returns flowOf(entries)

            // When
            val result = useCase(listOf(StatusState.WATCHING)).first()

            // Then
            assertEquals(3, result.size)
            assertEquals("天国大魔境", result[0].work.title)
            assertEquals("終物語", result[1].work.title)
            assertEquals("SPY×FAMILY", result[2].work.title)
        }

        @Test
        @DisplayName("空のリストが返される場合正しく処理される")
        fun handlesEmptyList() = runTest {
            // Given
            coEvery { repository.getLibraryEntries(listOf(StatusState.WATCHING), null) } returns flowOf(emptyList())

            // When
            val result = useCase(listOf(StatusState.WATCHING)).first()

            // Then
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("複数のステータスで取得できる")
        fun supportsMultipleStates() = runTest {
            // Given
            val states = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH)
            val entries = listOf(
                createLibraryEntry("entry1", "work1", "Work 1"),
                createLibraryEntry("entry2", "work2", "Work 2")
            )
            coEvery { repository.getLibraryEntries(states, null) } returns flowOf(entries)

            // When
            val result = useCase(states).first()

            // Then
            assertEquals(2, result.size)
        }
    }

    private fun createLibraryEntry(entryId: String, workId: String, title: String) = LibraryEntry(
        id = entryId,
        work = Work(
            id = workId,
            title = title,
            seasonName = null,
            seasonYear = null,
            media = null,
            malAnimeId = null,
            viewerStatusState = StatusState.WATCHING,
            image = null
        ),
        nextEpisode = null,
        statusState = StatusState.WATCHING
    )
}
