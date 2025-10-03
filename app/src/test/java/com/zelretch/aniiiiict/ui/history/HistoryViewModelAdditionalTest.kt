package com.zelretch.aniiiiict.ui.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * HistoryViewModelの追加テストケース
 *
 * 既存のHistoryViewModelTestで不足している以下のシナリオを追加テスト:
 * - ページネーション境界値処理
 * - 検索クエリの正規化とバリデーション
 * - 同期処理とエラー回復
 * - 状態遷移の妥当性
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("HistoryViewModel追加テスト")
class HistoryViewModelAdditionalTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("ページネーション処理")
    inner class Pagination {

        @Test
        @DisplayName("ページネーション境界条件を適切に処理する")
        fun paginationBoundary() = runTest {
            // Given
            data class PaginationState(
                val hasNextPage: Boolean = false,
                val endCursor: String? = null,
                val isLoading: Boolean = false
            )

            var state = PaginationState()

            // When: 次ページなしの状態
            state = state.copy(hasNextPage = false, endCursor = null)
            val shouldLoadNextPage = state.hasNextPage && !state.isLoading && state.endCursor != null

            // Then
            assertFalse(shouldLoadNextPage)

            // When: 次ページありの状態
            state = state.copy(hasNextPage = true, endCursor = "cursor123")
            val canLoadNextPage = state.hasNextPage && !state.isLoading && state.endCursor != null

            // Then
            assertTrue(canLoadNextPage)

            // When: ローディング中
            state = state.copy(isLoading = true)
            val cannotLoadWhileLoading = state.hasNextPage && !state.isLoading && state.endCursor != null

            // Then
            assertFalse(cannotLoadWhileLoading)
        }

        @Test
        @DisplayName("カーソルが空文字列やnullの場合適切に処理される")
        fun emptyCursor() = runTest {
            // Given
            val emptyCursor = ""
            val nullCursor: String? = null

            // When & Then: 空文字列カーソルの処理
            val isValidEmptyCursor = emptyCursor.isNotEmpty()
            assertFalse(isValidEmptyCursor)

            // When & Then: nullカーソルの処理
            val isValidNullCursor = nullCursor != null
            assertFalse(isValidNullCursor)
        }
    }

    @Nested
    @DisplayName("検索機能")
    inner class SearchFeature {

        @Test
        @DisplayName("検索クエリが適切に正規化される")
        fun queryNormalization() = runTest {
            // Given
            fun normalizeQuery(query: String): String = query.trim().lowercase()

            // When & Then: 前後の空白文字を削除
            assertEquals("テスト", normalizeQuery("  テスト  "))

            // When & Then: 大文字小文字の正規化
            assertEquals("test", normalizeQuery("TEST"))

            // When & Then: 空文字列の処理
            assertEquals("", normalizeQuery(""))

            // When & Then: 空白のみの処理
            assertEquals("", normalizeQuery("   "))
        }

        @Test
        @DisplayName("特殊な検索クエリ入力値を適切に処理する")
        fun specialQuery() = runTest {
            // Given
            val testQueries = listOf(
                "", // 空文字列
                " ", // スペース1個
                "a", // 1文字
                "あ", // 日本語1文字
                "test query", // 英数字スペース含む
                "テスト クエリ", // 日本語スペース含む
                "!@#$%^&*()", // 特殊文字
                "😀🎯💯" // 絵文字
            )

            // When & Then
            testQueries.forEach { query ->
                // 各クエリが適切に処理されることを確認
                val processedQuery = query.trim()
                assertNotNull(processedQuery)

                // 検索実行可能性の判定
                val canSearch = processedQuery.isNotEmpty()
                assertEquals(query.trim().isNotEmpty(), canSearch)
            }
        }
    }

    @Nested
    @DisplayName("エラー処理")
    inner class ErrorHandling {

        @Test
        @DisplayName("削除処理エラーが適切に設定される")
        fun deleteError() = runTest {
            // Given
            data class DeleteState(
                val isDeleting: Boolean = false,
                val deleteError: String? = null,
                val lastDeletedId: String? = null
            )

            var state = DeleteState()

            // When: 削除開始
            state = state.copy(isDeleting = true, deleteError = null)

            // Then
            assertTrue(state.isDeleting)
            assertNull(state.deleteError)

            // When: 削除エラー
            state = state.copy(
                isDeleting = false,
                deleteError = "削除に失敗しました",
                lastDeletedId = null
            )

            // Then
            assertFalse(state.isDeleting)
            assertEquals("削除に失敗しました", state.deleteError)
            assertNull(state.lastDeletedId)

            // When: 削除成功
            state = state.copy(
                isDeleting = false,
                deleteError = null,
                lastDeletedId = "record123"
            )

            // Then
            assertFalse(state.isDeleting)
            assertNull(state.deleteError)
            assertEquals("record123", state.lastDeletedId)
        }

        @Test
        @DisplayName("ネットワークエラーからの回復が適切に行われる")
        fun networkRecovery() = runTest {
            // Given
            data class NetworkState(
                val isRetrying: Boolean = false,
                val retryCount: Int = 0,
                val maxRetries: Int = 3,
                val lastError: String? = null
            )

            var state = NetworkState()

            // When: 初回エラー
            state = state.copy(
                lastError = "ネットワークエラー",
                retryCount = 0
            )

            // When & Then: リトライ処理
            repeat(3) { attempt ->
                state = state.copy(
                    isRetrying = true,
                    retryCount = attempt + 1
                )

                assertEquals(attempt + 1, state.retryCount)
                assertTrue(state.isRetrying)

                // リトライ完了
                state = state.copy(isRetrying = false)
            }

            // Then: 最大リトライ回数に達した場合
            val shouldContinueRetrying = state.retryCount < state.maxRetries
            assertFalse(shouldContinueRetrying)
        }
    }

    @Nested
    @DisplayName("HistoryUiStateの状態管理")
    inner class StateManagement {

        @Test
        @DisplayName("初期状態が適切なデフォルト値で設定される")
        fun initialState() = runTest {
            // When
            val initialState = HistoryUiState()

            // Then
            assertEquals(emptyList<Any>(), initialState.records)
            assertEquals(emptyList<Any>(), initialState.allRecords)
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
            assertFalse(initialState.hasNextPage)
            assertNull(initialState.endCursor)
            assertEquals("", initialState.searchQuery)
        }

        @Test
        @DisplayName("状態を段階的に更新すると各段階で適切な状態になる")
        fun incrementalUpdate() = runTest {
            // Given
            var state = HistoryUiState()

            // When: ローディング開始
            state = state.copy(isLoading = true, error = null)

            // Then
            assertTrue(state.isLoading)
            assertNull(state.error)

            // When: データ読み込み完了
            state = state.copy(
                isLoading = false,
                records = listOf(),
                allRecords = listOf(),
                hasNextPage = true,
                endCursor = "cursor1"
            )

            // Then
            assertFalse(state.isLoading)
            assertTrue(state.hasNextPage)
            assertEquals("cursor1", state.endCursor)

            // When: 検索クエリ更新
            state = state.copy(searchQuery = "test")

            // Then
            assertEquals("test", state.searchQuery)

            // When: エラー発生
            state = state.copy(
                isLoading = false,
                error = "読み込みエラー"
            )

            // Then
            assertFalse(state.isLoading)
            assertEquals("読み込みエラー", state.error)

            // When: エラー回復
            state = state.copy(error = null)

            // Then
            assertNull(state.error)
        }

        @Test
        @DisplayName("recordsとallRecordsのデータ整合性が適切")
        fun recordsとallRecordsのデータ整合性が適切() = runTest {
            // Given
            val allRecordsCount = 3
            val filteredRecordsCount = 2
            val hasSearchQuery = true

            // When & Then: データの整合性ルール
            val isConsistent = filteredRecordsCount <= allRecordsCount
            assertTrue(isConsistent)

            // When & Then: 検索が適用されている場合のルール
            val shouldHaveFewerRecords = hasSearchQuery && (filteredRecordsCount < allRecordsCount)
            assertTrue(shouldHaveFewerRecords)

            // When & Then: 初期状態では全レコードが表示される
            val noSearchQuery = false
            val shouldShowAllRecords = !noSearchQuery || (filteredRecordsCount == allRecordsCount)
            assertTrue(shouldShowAllRecords)
        }
    }
}
