package com.zelretch.aniiiiict.ui.history

import androidx.lifecycle.ViewModel
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

    fun loadRecords() {
        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true, error = null) }

            loadRecordsUseCase()
                .onSuccess { result ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            allRecords = result.records,
                            records = searchRecordsUseCase(result.records, currentState.searchQuery),
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "HistoryViewModel.loadRecords")
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    Timber.e(e, "記録の読み込みに失敗: $msg")
                }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage || currentState.endCursor == null) {
            return
        }

        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true) }

            loadRecordsUseCase(currentState.endCursor)
                .onSuccess { result ->
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
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "HistoryViewModel.loadNextPage")
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    Timber.e(e, "次ページの読み込みに失敗: $msg")
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                records = searchRecordsUseCase(currentState.allRecords, query)
            )
        }
    }

    fun deleteRecord(recordId: String) {
        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deleteRecordUseCase(recordId)
                .onSuccess {
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
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "HistoryViewModel.deleteRecord")
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    Timber.e(e, "レコード削除に失敗: $msg")
                }
        }
    }

    fun showRecordDetail(record: Record) {
        _uiState.update {
            it.copy(
                selectedRecord = record,
                isDetailModalVisible = true
            )
        }
    }

    fun hideRecordDetail() {
        _uiState.update {
            it.copy(
                selectedRecord = null,
                isDetailModalVisible = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
