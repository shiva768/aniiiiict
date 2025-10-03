package com.zelretch.aniiiiict.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiict.domain.usecase.SearchRecordsUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import com.zelretch.aniiiiict.ui.base.launchWithMinLoadingTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * 視聴履歴画面のUI状態
 *
 * Note: ページネーション、検索、モーダル表示など複雑な状態管理があるため、
 * 現時点では従来のUiStateパターンを維持し、BaseViewModelのみ削除する。
 * 将来的にUiState<T>パターンへの完全移行を検討。
 */
data class HistoryUiState(
    val records: List<Record> = emptyList(),
    val allRecords: List<Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val searchQuery: String = "",
    val selectedRecord: Record? = null,
    val isDetailModalVisible: Boolean = false
)

/**
 * 視聴履歴画面のViewModel
 *
 * Now in Android パターンへの移行:
 * - BaseViewModelを削除し、明示的なエラーハンドリング
 * - launchWithMinLoadingTimeで最小ローディング時間を保証
 * - ErrorMapperによるユーザー向けメッセージ変換
 *
 * TODO: 将来的にUiState<T>パターンへの完全移行を検討
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val loadRecordsUseCase: LoadRecordsUseCase,
    private val searchRecordsUseCase: SearchRecordsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    /**
     * レコードを読み込む
     */
    fun loadRecords() {
        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = loadRecordsUseCase()
            val allRecords = result.records

            _uiState.update { currentState ->
                currentState.copy(
                    allRecords = allRecords,
                    records = searchRecordsUseCase(allRecords, currentState.searchQuery),
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * 次のページを読み込む
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage || currentState.endCursor == null) {
            return
        }

        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true) }

            val result = loadRecordsUseCase(currentState.endCursor)

            _uiState.update { state ->
                val newAllRecords = state.allRecords + result.records
                state.copy(
                    allRecords = newAllRecords,
                    records = searchRecordsUseCase(newAllRecords, state.searchQuery),
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor,
                    isLoading = false
                )
            }
        }
    }

    /**
     * 検索クエリを更新する
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                records = searchRecordsUseCase(currentState.allRecords, query)
            )
        }
    }

    /**
     * レコードを削除する
     */
    fun deleteRecord(recordId: String) {
        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val success = deleteRecordUseCase(recordId)

            if (success) {
                _uiState.update { currentState ->
                    val newAllRecords = currentState.allRecords.filter { it.id != recordId }
                    currentState.copy(
                        allRecords = newAllRecords,
                        records = searchRecordsUseCase(newAllRecords, currentState.searchQuery),
                        isLoading = false,
                        error = null
                    )
                }
                Timber.i("レコードを削除しました: $recordId")
            } else {
                _uiState.update { it.copy(isLoading = false, error = "記録の削除に失敗しました") }
                Timber.w("レコード削除に失敗: $recordId")
            }
        }
    }

    /**
     * レコード詳細を表示する
     */
    fun showRecordDetail(record: Record) {
        _uiState.update {
            it.copy(
                selectedRecord = record,
                isDetailModalVisible = true
            )
        }
    }

    /**
     * レコード詳細を非表示にする
     */
    fun hideRecordDetail() {
        _uiState.update {
            it.copy(
                selectedRecord = null,
                isDetailModalVisible = false
            )
        }
    }

    /**
     * エラーをクリアする
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
