package com.zelretch.aniiiiict.ui.animedetail

/**
 * 外部リンクとして開ける形に整える。
 * Annict は未設定の URL を空文字で返すことがあるので、空なら null（リンクを出さない）。
 * スキームが無ければ https を補う。
 */
internal fun normalizeExternalUrl(url: String?): String? {
    val trimmed = url?.trim()
    if (trimmed.isNullOrEmpty()) return null
    return if (trimmed.contains("://")) trimmed else "https://$trimmed"
}
