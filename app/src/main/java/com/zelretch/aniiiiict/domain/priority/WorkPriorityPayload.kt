package com.zelretch.aniiiiict.domain.priority

import com.zelretch.aniiiiict.data.model.WorkPriority
import java.util.Base64

/**
 * 優先度（Tier）を別端末へ QR / テキストで渡すための文字列形式。
 *
 * 形式: `aniiiiict-priority/1|1:123,456|2:789|3:`
 * - 先頭のバージョンが違うものは読まない（形式を変えたときに古いアプリが誤読しないように）
 * - 1〜3 は Tier1〜3。無印は載せない
 * - 作品 ID は Annict のグローバル ID（base64 の "Work-N"）を N に縮める。縮められない ID はそのまま載せる
 */
object WorkPriorityPayload {

    private const val HEADER = "aniiiiict-priority/1"
    private const val WORK_PREFIX = "Work-"

    private val tiers = mapOf(
        "1" to WorkPriority.TIER1,
        "2" to WorkPriority.TIER2,
        "3" to WorkPriority.TIER3
    )

    fun encode(priorities: Map<String, WorkPriority>): String {
        val sections = tiers.map { (key, priority) ->
            val ids = priorities.filterValues { it == priority }.keys.map(::compact).sorted()
            "$key:${ids.joinToString(",")}"
        }
        return (listOf(HEADER) + sections).joinToString("|")
    }

    /** 読めない文字列（別アプリの QR・形式違い）は null */
    fun decode(payload: String): Map<String, WorkPriority>? {
        val parts = payload.trim().split("|")
        if (parts.first() != HEADER) return null
        val result = mutableMapOf<String, WorkPriority>()
        for (section in parts.drop(1)) {
            val key = section.substringBefore(":", missingDelimiterValue = "")
            val priority = tiers[key] ?: return null
            section.substringAfter(":").split(",").filter { it.isNotBlank() }.forEach { id ->
                result[expand(id)] = priority
            }
        }
        return result
    }

    private fun compact(workId: String): String {
        val decoded = runCatching { String(Base64.getDecoder().decode(workId)) }.getOrNull() ?: return workId
        val number = decoded.removePrefix(WORK_PREFIX)
        val isWorkNumber = decoded.startsWith(WORK_PREFIX) && number.isNotEmpty() && number.all { it.isDigit() }
        // パディングの有無などで戻したときに同じ ID にならないなら縮めない
        return if (isWorkNumber && expand(number) == workId) number else workId
    }

    private fun expand(id: String): String = if (id.all { it.isDigit() }) {
        Base64.getEncoder().encodeToString("$WORK_PREFIX$id".toByteArray())
    } else {
        id
    }
}
