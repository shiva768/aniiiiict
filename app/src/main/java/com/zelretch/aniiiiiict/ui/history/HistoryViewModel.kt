package com.zelretch.aniiiiiict.ui.history

import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val records: List<Record> = emptyList(),
    val allRecords: List<Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AnnictRepository,
    logger: Logger
) : BaseViewModel(logger) {
    private val TAG = "HistoryViewModel"
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
                val allRecords = result.records
                it.copy(
                    allRecords = allRecords,
                    records = filterRecords(allRecords, it.searchQuery),
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
                val newAllRecords = it.allRecords + result.records
                it.copy(
                    allRecords = newAllRecords,
                    records = filterRecords(newAllRecords, it.searchQuery),
                    hasNextPage = result.hasNextPage,
                    endCursor = result.endCursor
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                records = filterRecords(currentState.allRecords, query)
            )
        }

        _uiState.update {
            it.copy(
                hasNextPage = false,
                endCursor = null
            )
        }
        loadRecords()
    }

    private fun filterRecords(records: List<Record>, query: String): List<Record> {
        if (query.isEmpty()) return records
        return records.filter { record ->
            record.work.title.contains(query, ignoreCase = true)
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(recordId)
                _uiState.update { currentState ->
                    val newAllRecords = currentState.allRecords.filter { it.id != recordId }
                    currentState.copy(
                        allRecords = newAllRecords,
                        records = filterRecords(newAllRecords, currentState.searchQuery)
                    )
                }
            } catch (e: Exception) {
                logger.logError(TAG, e, "記録の削除に失敗")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "記録の削除に失敗しました"
                    )
                }
            }
        }
    }
} 