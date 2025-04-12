package com.zelretch.aniiiiiict.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val records: List<Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeletingRecord: Boolean = false,
    val hasMoreData: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AnnictRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 30

    init {
        loadRecords()
    }

    fun loadRecords(isRefresh: Boolean = false) {
        if (_uiState.value.isLoading) return
        if (isRefresh) {
            currentPage = 1
            _uiState.value = _uiState.value.copy(hasMoreData = true)
        }
        if (!_uiState.value.hasMoreData && !isRefresh) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val records = repository.getRecords(pageSize)
                val currentRecords = if (isRefresh) records else {
                    _uiState.value.records + records
                }

                _uiState.value = _uiState.value.copy(
                    records = currentRecords,
                    isLoading = false,
                    hasMoreData = records.size >= pageSize
                )
                if (_uiState.value.hasMoreData) currentPage++
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "記録履歴の取得に失敗")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "記録履歴の取得に失敗しました: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeletingRecord = true)

                val success = repository.deleteRecord(recordId)
                if (success) {
                    // 現在の記録リストから削除したものを除外
                    val updatedRecords = _uiState.value.records.filter { it.id != recordId }
                    _uiState.value = _uiState.value.copy(
                        records = updatedRecords,
                        isDeletingRecord = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeletingRecord = false,
                        error = "記録の削除に失敗しました"
                    )
                }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "記録削除中にエラー: $recordId")
                _uiState.value = _uiState.value.copy(
                    isDeletingRecord = false,
                    error = "記録の削除に失敗しました: ${e.localizedMessage}"
                )
            }
        }
    }
} 