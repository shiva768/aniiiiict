package com.zelretch.aniiiiiict.ui.history

import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val records: List<Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AnnictRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    fun loadRecords() {
        executeWithLoading {
            val result = repository.getRecords(null)
            _uiState.update {
                it.copy(
                    records = result.records,
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage || currentState.endCursor == null) {
            return
        }

        executeWithLoading {
            val result = repository.getRecords(currentState.endCursor)
            _uiState.update {
                it.copy(
                    records = it.records + result.records,
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor
                )
            }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(recordId)
                _uiState.update { currentState ->
                    currentState.copy(
                        records = currentState.records.filter { it.id != recordId }
                    )
                }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "記録の削除に失敗")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "記録の削除に失敗しました"
                    )
                }
            }
        }
    }
} 