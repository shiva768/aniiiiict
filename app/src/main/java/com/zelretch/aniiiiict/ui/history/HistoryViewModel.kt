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
import kotlinx.coroutines.launch
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
    val isDetailModalVisible: Boolean = false,
    // スワイプ削除で楽観的にリストから外し、取り消し待ちのレコード（Snackbarの取り消し対象）
    val pendingDeletion: Record? = null
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

    /**
     * スワイプ削除：API呼び出しはすぐに行わず、まずリストから楽観的に外して取り消し待ちにする。
     * Snackbar がタイムアウトしたら [commitDelete]、取り消されたら [undoDelete] が呼ばれる。
     */
    fun deleteRecord(recordId: String) {
        val record = _uiState.value.allRecords.find { it.id == recordId } ?: return
        // 直前の取り消し待ちがあれば先に確定する
        _uiState.value.pendingDeletion?.let { commitDeleteInternal(it) }
        _uiState.update { currentState ->
            val newAllRecords = currentState.allRecords.filter { it.id != recordId }
            currentState.copy(
                allRecords = newAllRecords,
                records = searchRecordsUseCase(newAllRecords, currentState.searchQuery),
                pendingDeletion = record
            )
        }
    }

    /** Snackbarの「取り消し」：楽観的に外したレコードをリストへ戻す。 */
    fun undoDelete() {
        val pending = _uiState.value.pendingDeletion ?: return
        _uiState.update { currentState ->
            val restored = (currentState.allRecords + pending).sortedByDescending { it.createdAt }
            currentState.copy(
                allRecords = restored,
                records = searchRecordsUseCase(restored, currentState.searchQuery),
                pendingDeletion = null
            )
        }
    }

    /** Snackbarがタイムアウト：取り消し待ちのレコードを実際にAPIで削除する。 */
    fun commitDelete() {
        val pending = _uiState.value.pendingDeletion ?: return
        _uiState.update { it.copy(pendingDeletion = null) }
        commitDeleteInternal(pending)
    }

    private fun commitDeleteInternal(record: Record) {
        viewModelScope.launch {
            deleteRecordUseCase(record.id)
                .onSuccess { Timber.i("レコードを削除しました: ${record.id}") }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "HistoryViewModel.deleteRecord")
                    _uiState.update { currentState ->
                        // 失敗したらリストへ戻す
                        val restored = (currentState.allRecords + record).sortedByDescending { it.createdAt }
                        currentState.copy(
                            allRecords = restored,
                            records = searchRecordsUseCase(restored, currentState.searchQuery),
                            error = msg
                        )
                    }
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
