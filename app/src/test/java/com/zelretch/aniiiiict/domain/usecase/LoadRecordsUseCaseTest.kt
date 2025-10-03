package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("LoadRecordsUseCase")
class LoadRecordsUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: LoadRecordsUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = LoadRecordsUseCase(repository)
    }

    @Nested
    @DisplayName("記録一覧のロード")
    inner class LoadRecords {

        @Test
        @DisplayName("レコードが存在し次ページもある場合正しく変換される")
        fun withRecordsAndNextPage() = runTest {
            // Given
            val fakeRecords = listOf(
                Record(
                    id = "id1",
                    comment = "test1",
                    rating = 4.5,
                    createdAt = ZonedDateTime.now(),
                    episode = dummyEpisode("ep1"),
                    work = dummyWork("w1")
                ),
                Record(
                    id = "id2",
                    comment = "test2",
                    rating = 5.0,
                    createdAt = ZonedDateTime.now(),
                    episode = dummyEpisode("ep2"),
                    work = dummyWork("w2")
                )
            )
            val paginated = PaginatedRecords(
                records = fakeRecords,
                hasNextPage = true,
                endCursor = "CURSOR123"
            )
            coEvery { repository.getRecords(null) } returns paginated

            // When
            val result = useCase()

            // Then
            assertEquals(fakeRecords, result.records)
            assertEquals(true, result.hasNextPage)
            assertEquals("CURSOR123", result.endCursor)
        }

        @Test
        @DisplayName("レコードが空の場合正しいページ情報が返る")
        fun withEmptyRecords() = runTest {
            // Given
            val paginated = PaginatedRecords(
                records = emptyList(),
                hasNextPage = false,
                endCursor = null
            )
            coEvery { repository.getRecords(null) } returns paginated

            // When
            val result = useCase()

            // Then
            assertEquals(emptyList<Record>(), result.records)
            assertEquals(false, result.hasNextPage)
            assertNull(result.endCursor)
        }
    }

    private fun dummyEpisode(id: String) = Episode(
        id = id,
        number = 1,
        numberText = "1",
        title = "Dummy Episode",
        viewerDidTrack = false
    )

    private fun dummyWork(id: String) = Work(
        id = id,
        title = "Dummy Work",
        seasonName = SeasonName.SPRING,
        seasonYear = 2024,
        media = "TV",
        mediaText = "テレビ",
        viewerStatusState = StatusState.WATCHING,
        seasonNameText = "春",
        image = null
    )
}
