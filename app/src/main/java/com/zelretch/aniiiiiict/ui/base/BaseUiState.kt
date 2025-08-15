package com.zelretch.aniiiiiict.ui.base

/**
 * 共通のUI状態プロパティを持つ基本クラス
 */
open class BaseUiState(
    open val isLoading: Boolean = false,
    open val error: String? = null,
)
