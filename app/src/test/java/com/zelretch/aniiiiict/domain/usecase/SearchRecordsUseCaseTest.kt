package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class レコード検索ユースケーステスト : BehaviorSpec({
    val useCase = SearchRecordsUseCase()

    given("複数のRecordがあるとき") {
        `when`("タイトルで検索") {
            then("該当するRecordだけ返る") {
                val records = listOf(
                    Record(
                        "1",
                        null,
                        null,
                        ZonedDateTime.now(),
                        Episode("ep1"),
                        Work("w1", "テスト", viewerStatusState = StatusState.NO_STATE)
                    ),
                    Record(
                        "2",
                        null,
                        null,
                        ZonedDateTime.now(),
                        Episode("ep2"),
                        Work("w2", "サンプル", viewerStatusState = StatusState.NO_STATE)
                    )
                )
                val result = useCase(records, "テスト")
                result.size shouldBe 1
                result[0].work.title shouldBe "テスト"
            }
        }
    }
})
