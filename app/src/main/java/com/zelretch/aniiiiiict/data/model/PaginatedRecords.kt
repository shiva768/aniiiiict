package com.zelretch.aniiiiiict.data.model

/**
 * ページネーション情報を含む記録リストを表すデータクラス
 *
 * @property records 記録のリスト
 * @property hasNextPage 次のページが存在するかどうか
 * @property endCursor 次のページを取得するためのカーソル
 */
data class PaginatedRecords(
    val records: List<Record>,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null
)
