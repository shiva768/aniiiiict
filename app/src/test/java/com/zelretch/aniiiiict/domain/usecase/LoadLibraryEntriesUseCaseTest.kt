package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.LibraryEntryEntity
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LoadLibraryEntriesUseCase")
class LoadLibraryEntriesUseCaseTest {

    private lateinit var libraryEntryDao: LibraryEntryDao
    private lateinit var useCase: LoadLibraryEntriesUseCase

    @BeforeEach
    fun setup() {
        libraryEntryDao = mockk()
        useCase = LoadLibraryEntriesUseCase(libraryEntryDao)
    }

    @Nested
    @DisplayName("Roomからの読み込み")
    inner class LoadFromRoom {

        @Test
        @DisplayName("Roomのデータを正常に返す")
        fun returnsEntriesFromRoom() = runTest {
            // Given
            val entities = listOf(createFakeEntity("entry1"), createFakeEntity("entry2"))
            coEvery { libraryEntryDao.getAll() } returns entities

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }

        @Test
        @DisplayName("Roomが空の場合は空リストを返す")
        fun returnsEmptyListWhenRoomIsEmpty() = runTest {
            // Given
            coEvery { libraryEntryDao.getAll() } returns emptyList()

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(emptyList<LibraryEntry>(), result.getOrThrow())
        }

        @Test
        @DisplayName("例外発生時はfailureが返る")
        fun returnsFailureOnException() = runTest {
            // Given
            val exception = RuntimeException("DB error")
            coEvery { libraryEntryDao.getAll() } throws exception

            // When
            val result = useCase()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
    }

    private fun createFakeEntity(id: String) = LibraryEntryEntity(
        id = id,
        workId = "work_$id",
        workTitle = "Work $id",
        workMedia = null,
        workSeasonName = null,
        workSeasonYear = null,
        workViewerStatusState = StatusState.WANNA_WATCH.name,
        workMalAnimeId = null,
        workNoEpisodes = false,
        workImageUrl = null,
        nextEpisodeId = null,
        nextEpisodeNumber = null,
        nextEpisodeNumberText = null,
        nextEpisodeTitle = null,
        statusState = null,
        fetchedAt = System.currentTimeMillis()
    )

    @Suppress("unused")
    private fun createFakeLibraryEntry(id: String) = LibraryEntry(
        id = id,
        work = Work(
            id = "work_$id",
            title = "Work $id",
            viewerStatusState = StatusState.WANNA_WATCH
        ),
        nextEpisode = null,
        statusState = StatusState.WANNA_WATCH
    )
}
