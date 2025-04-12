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
    val hasNextPage: Boolean = false,
    val endCursor: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AnnictRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = repository.getRecords()
                _uiState.value = _uiState.value.copy(
                    records = result.records,
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor,
                    isLoading = false
                )
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "記録履歴の取得に失敗")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "記録履歴の取得に失敗しました: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.endCursor == null || currentState.isLoading) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoading = true, error = null)

                val result = repository.getRecords(after = currentState.endCursor)
                _uiState.value = _uiState.value.copy(
                    records = currentState.records + result.records,
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor,
                    isLoading = false
                )
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "次のページの取得に失敗")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "次のページの取得に失敗しました: ${e.localizedMessage}"
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