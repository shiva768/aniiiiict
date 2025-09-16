package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class レコード読み込みユースケーステスト : BehaviorSpec({
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

    前提("記録一覧をロードする") {
        場合("レコードが存在し、次ページもある場合") {
            そのとき("RecordsResultに正しく変換される") {
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
        場合("レコードが空の場合") {
            そのとき("空リストと正しいページ情報が返る") {
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
