package com.zelretch.aniiiiict.domain.priority

import com.zelretch.aniiiiict.data.model.WorkPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64

@DisplayName("WorkPriorityPayload")
class WorkPriorityPayloadTest {

    private fun workId(n: Int) = Base64.getEncoder().encodeToString("Work-$n".toByteArray())

    @Test
    @DisplayName("作品 ID を番号に縮めて Tier ごとにまとめる")
    fun encodeCompactsIds() {
        val payload = WorkPriorityPayload.encode(
            mapOf(workId(456) to WorkPriority.TIER1, workId(123) to WorkPriority.TIER1, workId(9) to WorkPriority.TIER3)
        )

        assertEquals("aniiiiict-priority/1|1:123,456|2:|3:9", payload)
    }

    @Test
    @DisplayName("書き出して読み込むと元に戻る（縮められない ID もそのまま）")
    fun roundTrip() {
        val priorities = mapOf(
            workId(1) to WorkPriority.TIER1,
            workId(20) to WorkPriority.TIER2,
            workId(300) to WorkPriority.TIER3,
            "raw-id" to WorkPriority.TIER2
        )

        assertEquals(priorities, WorkPriorityPayload.decode(WorkPriorityPayload.encode(priorities)))
    }

    @Test
    @DisplayName("何も付いていなければ空で往復する")
    fun emptyRoundTrip() {
        assertEquals(
            emptyMap<String, WorkPriority>(),
            WorkPriorityPayload.decode(WorkPriorityPayload.encode(emptyMap()))
        )
    }

    @Test
    @DisplayName("ヘッダーやバージョンが違う・Tier が不明なら読まない")
    fun rejectsUnknown() {
        assertNull(WorkPriorityPayload.decode("https://example.com"))
        assertNull(WorkPriorityPayload.decode("aniiiiict-priority/2|1:1"))
        assertNull(WorkPriorityPayload.decode("aniiiiict-priority/1|4:1"))
        assertNull(WorkPriorityPayload.decode("aniiiiict-priority/1|broken"))
    }

    @Test
    @DisplayName("前後の空白・改行は無視する（テキスト貼り付け用）")
    fun trimsWhitespace() {
        assertEquals(
            mapOf(workId(5) to WorkPriority.TIER2),
            WorkPriorityPayload.decode("  aniiiiict-priority/1|1:|2:5|3:\n")
        )
    }

    @Test
    @DisplayName("パディング無しなど、戻すと同じにならない ID は縮めずそのまま載せる")
    fun keepsIdThatDoesNotRoundTrip() {
        val unpadded = workId(12345).trimEnd('=')

        val payload = WorkPriorityPayload.encode(mapOf(unpadded to WorkPriority.TIER1))

        assertEquals("aniiiiict-priority/1|1:$unpadded|2:|3:", payload)
        assertEquals(mapOf(unpadded to WorkPriority.TIER1), WorkPriorityPayload.decode(payload))
    }
}
