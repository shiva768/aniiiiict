package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.LibraryEntryEntity
import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import com.zelretch.aniiiiict.data.local.WorkPriorityEntity
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkPriority
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
    private lateinit var workPriorityDao: WorkPriorityDao
    private lateinit var useCase: LoadLibraryEntriesUseCase

    @BeforeEach
    fun setup() {
        libraryEntryDao = mockk()
        workPriorityDao = mockk()
        coEvery { workPriorityDao.getAll() } returns emptyList()
        useCase = LoadLibraryEntriesUseCase(libraryEntryDao, workPriorityDao)
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
        @DisplayName("優先度テーブルの値が workId で重ねられ、無い作品はふつうになる")
        fun overlaysPriority() = runTest {
            // Given
            coEvery { libraryEntryDao.getAll() } returns listOf(createFakeEntity("entry1"), createFakeEntity("entry2"))
            coEvery { workPriorityDao.getAll() } returns listOf(
                WorkPriorityEntity(workId = "work_entry1", priority = WorkPriority.FEATURED.name)
            )

            // When
            val entries = useCase().getOrThrow()

            // Then
            assertEquals(WorkPriority.FEATURED, entries[0].priority)
            assertEquals(WorkPriority.NORMAL, entries[1].priority)
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
