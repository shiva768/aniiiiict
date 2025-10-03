package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("SearchRecordsUseCase")
class SearchRecordsUseCaseTest {

    private lateinit var useCase: SearchRecordsUseCase

    @BeforeEach
    fun setup() {
        useCase = SearchRecordsUseCase()
    }

    @Nested
    @DisplayName("検索")
    inner class Search {

        @Test
        @DisplayName("タイトルで検索すると該当するRecordだけ返る")
        fun タイトルで検索すると該当するRecordだけ返る() {
            // Given
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

            // When
            val result = useCase(records, "テスト")

            // Then
            assertEquals(1, result.size)
            assertEquals("テスト", result[0].work.title)
        }
    }
}
