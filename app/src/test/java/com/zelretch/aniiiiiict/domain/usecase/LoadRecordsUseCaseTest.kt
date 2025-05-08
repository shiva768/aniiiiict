package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class LoadRecordsUseCaseTest : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = LoadRecordsUseCase(repository)

    fun dummyEpisode(id: String) = Episode(
        id = id,
        number = 1,
        numberText = "1",
        title = "Dummy Episode",
        viewerDidTrack = false
    )

    fun dummyWork(id: String) = Work(
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

    given("記録一覧をロードする") {
        `when`("レコードが存在し、次ページもある場合") {
            then("RecordsResultに正しく変換される") {
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
                val result = runBlocking { useCase() }
                result.records shouldBe fakeRecords
                result.hasNextPage shouldBe true
                result.endCursor shouldBe "CURSOR123"
            }
        }
        `when`("レコードが空の場合") {
            then("空リストと正しいページ情報が返る") {
                val paginated = PaginatedRecords(
                    records = emptyList(),
                    hasNextPage = false,
                    endCursor = null
                )
                coEvery { repository.getRecords(null) } returns paginated
                val result = runBlocking { useCase() }
                result.records shouldBe emptyList()
                result.hasNextPage shouldBe false
                result.endCursor shouldBe null
            }
        }
    }
})
