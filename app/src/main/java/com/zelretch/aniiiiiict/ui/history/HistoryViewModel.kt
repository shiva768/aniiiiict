package com.zelretch.aniiiiiict.ui.history

import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiiict.domain.usecase.SearchRecordsUseCase
import com.zelretch.aniiiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HistoryUiState(
    val records: List<Record> = emptyList(),
    val allRecords: List<Record> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val searchQuery: String = ""
) : BaseUiState(isLoading, error)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val loadRecordsUseCase: LoadRecordsUseCase,
    private val searchRecordsUseCase: SearchRecordsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase
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
            val result = loadRecordsUseCase()
            _uiState.update {
                val allRecords = result.records
                it.copy(
                    allRecords = allRecords,
                    records = searchRecordsUseCase(allRecords, it.searchQuery),
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
            val result = loadRecordsUseCase(currentState.endCursor)
            _uiState.update {
                val newAllRecords = it.allRecords + result.records
                it.copy(
                    allRecords = newAllRecords,
                    records = searchRecordsUseCase(newAllRecords, it.searchQuery),
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
                records = searchRecordsUseCase(currentState.allRecords, query)
            )
        }
    }

    fun deleteRecord(recordId: String) {
        executeWithLoading {
            deleteRecordUseCase(recordId)
            _uiState.update { currentState ->
                val newAllRecords = currentState.allRecords.filter { it.id != recordId }
                currentState.copy(
                    allRecords = newAllRecords,
                    records = searchRecordsUseCase(newAllRecords, currentState.searchQuery)
                )
            }
        }
    }
}
