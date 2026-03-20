package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.LibraryEntryEntity
import com.zelretch.aniiiiict.data.model.LibraryEntriesPage
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryFetchParams
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

    private lateinit var repository: AnnictRepository
    private lateinit var libraryEntryDao: LibraryEntryDao
    private lateinit var useCase: LoadLibraryEntriesUseCase

    private val defaultParams = LibraryFetchParams(
        selectedStates = listOf(StatusState.WANNA_WATCH, StatusState.ON_HOLD),
        seasonFromYear = 2020,
        seasonFromName = SeasonName.SPRING
    )

    @BeforeEach
    fun setup() {
        repository = mockk()
        libraryEntryDao = mockk()
        useCase = LoadLibraryEntriesUseCase(repository, libraryEntryDao)
    }

    @Nested
    @DisplayName("キャッシュ制御")
    inner class CacheControl {

        @Test
        @DisplayName("キャッシュヒット時はAPIを呼ばずキャッシュを返す")
        fun cacheHitReturnsCache() = runTest {
            // Given
            val cachedEntities = listOf(createFakeEntity("entry1", defaultParams.hash))
            coEvery { libraryEntryDao.getByHash(defaultParams.hash) } returns cachedEntities

            // When
            val result = useCase(defaultParams, forceRefresh = false)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
            coVerify(exactly = 0) { repository.getLibraryEntries(any(), any(), any()) }
        }

        @Test
        @DisplayName("キャッシュミス時はAPIを呼びRoomに保存する")
        fun cacheMissFetchesFromApi() = runTest {
            // Given
            val fakeEntry = createFakeLibraryEntry("entry1")
            coEvery { libraryEntryDao.getByHash(defaultParams.hash) } returns emptyList()
            coEvery {
                repository.getLibraryEntries(any(), any(), null)
            } returns Result.success(LibraryEntriesPage(listOf(fakeEntry), false, null))
            coEvery { libraryEntryDao.deleteByHash(defaultParams.hash) } returns Unit
            coEvery { libraryEntryDao.insertAll(any()) } returns Unit

            // When
            val result = useCase(defaultParams, forceRefresh = false)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
            coVerify { repository.getLibraryEntries(any(), defaultParams.seasonFrom, null) }
            coVerify { libraryEntryDao.insertAll(any()) }
        }

        @Test
        @DisplayName("forceRefresh=trueの場合キャッシュ有でもAPIを呼ぶ")
        fun forceRefreshIgnoresCache() = runTest {
            // Given
            val cachedEntities = listOf(createFakeEntity("entry1", defaultParams.hash))
            val freshEntry = createFakeLibraryEntry("entry2")
            coEvery { libraryEntryDao.getByHash(defaultParams.hash) } returns cachedEntities
            coEvery {
                repository.getLibraryEntries(any(), any(), null)
            } returns Result.success(LibraryEntriesPage(listOf(freshEntry), false, null))
            coEvery { libraryEntryDao.deleteByHash(defaultParams.hash) } returns Unit
            coEvery { libraryEntryDao.insertAll(any()) } returns Unit

            // When
            val result = useCase(defaultParams, forceRefresh = true)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("entry2", result.getOrThrow()[0].id)
            coVerify { repository.getLibraryEntries(any(), any(), null) }
        }

        @Test
        @DisplayName("複数ページある場合は全ページ取得する")
        fun fetchesAllPagesWhenMultiplePages() = runTest {
            // Given
            val page1Entry = createFakeLibraryEntry("entry1")
            val page2Entry = createFakeLibraryEntry("entry2")
            coEvery { libraryEntryDao.getByHash(defaultParams.hash) } returns emptyList()
            coEvery {
                repository.getLibraryEntries(any(), any(), null)
            } returns Result.success(LibraryEntriesPage(listOf(page1Entry), true, "cursor1"))
            coEvery {
                repository.getLibraryEntries(any(), any(), "cursor1")
            } returns Result.success(LibraryEntriesPage(listOf(page2Entry), false, null))
            coEvery { libraryEntryDao.deleteByHash(defaultParams.hash) } returns Unit
            coEvery { libraryEntryDao.insertAll(any()) } returns Unit

            // When
            val result = useCase(defaultParams, forceRefresh = false)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
            coVerify { repository.getLibraryEntries(any(), any(), null) }
            coVerify { repository.getLibraryEntries(any(), any(), "cursor1") }
        }

        @Test
        @DisplayName("APIエラー時はfailureが返る")
        fun apiErrorReturnsFailure() = runTest {
            // Given
            val exception = RuntimeException("API error")
            coEvery { libraryEntryDao.getByHash(defaultParams.hash) } returns emptyList()
            coEvery {
                repository.getLibraryEntries(any(), any(), any())
            } returns Result.failure(exception)

            // When
            val result = useCase(defaultParams, forceRefresh = false)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
    }

    private fun createFakeEntity(id: String, fetchParamsHash: String) = LibraryEntryEntity(
        id = id,
        fetchParamsHash = fetchParamsHash,
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
