package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LoadLibraryEntriesUseCase")
class LoadLibraryEntriesUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: LoadLibraryEntriesUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = LoadLibraryEntriesUseCase(repository)
    }

    @Nested
    @DisplayName("ライブラリエントリーのロード")
    inner class LoadLibraryEntries {

        @Test
        @DisplayName("視聴中作品一覧を正しく取得できる")
        fun withWatchingEntries() = runTest {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "天国大魔境"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "終物語"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { repository.getLibraryEntries(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            // When
            val result = useCase(listOf(StatusState.WATCHING)).getOrThrow()

            // Then
            assertEquals(2, result.size)
            assertEquals("entry1", result[0].id)
            assertEquals("天国大魔境", result[0].work.title)
            assertEquals("entry2", result[1].id)
            assertEquals("終物語", result[1].work.title)
        }

        @Test
        @DisplayName("空の結果を正しく処理できる")
        fun withEmptyResult() = runTest {
            // Given
            coEvery { repository.getLibraryEntries(listOf(StatusState.WATCHING)) } returns Result.success(emptyList())

            // When
            val result = useCase(listOf(StatusState.WATCHING)).getOrThrow()

            // Then
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("複数のステータスで取得できる")
        fun withMultipleStates() = runTest {
            // Given
            val states = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH)
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Work 1"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Work 2"),
                    nextEpisode = null,
                    statusState = StatusState.WANNA_WATCH
                )
            )
            coEvery { repository.getLibraryEntries(states) } returns Result.success(fakeEntries)

            // When
            val result = useCase(states).getOrThrow()

            // Then
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("デフォルトでWATCHINGステートを使用する")
        fun usesWatchingStateByDefault() = runTest {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Test Work"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { repository.getLibraryEntries(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            // When
            val result = useCase().getOrThrow()

            // Then
            assertEquals(1, result.size)
        }
    }

    private fun createFakeWork(id: String, title: String) = Work(
        id = id,
        title = title,
        seasonName = null,
        seasonYear = null,
        media = null,
        malAnimeId = null,
        viewerStatusState = StatusState.WATCHING,
        image = null
    )
}
