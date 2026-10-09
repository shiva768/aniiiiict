package com.zelretch.aniiiiict.data.model

/**
 * Annict のステータスとは別に、端末内だけで持つ作品の優先度。
 * 宣言順がライブラリでの並び順（Tier1 → Tier2 → Tier3 → 無印）。
 */
enum class WorkPriority {
    TIER1,
    TIER2,
    TIER3,
    NONE
}
