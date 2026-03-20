package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.domain.sync.LibrarySyncService
import com.zelretch.aniiiiict.domain.sync.SyncStatus
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LibraryFilterState(
    val selectedMedia: Set<String> = emptySet(),
    val selectedStatuses: Set<StatusState> = emptySet(),
    val searchQuery: String = ""
)

data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val allEntries: List<LibraryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val filterState: LibraryFilterState = LibraryFilterState(),
    val availableMedia: List<String> = emptyList(),
    val availableStatuses: List<StatusState> = emptyList(),
    val isFilterVisible: Boolean = false,
    val selectedEntry: LibraryEntry? = null,
    val isDetailModalVisible: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase,
    private val librarySyncService: LibrarySyncService,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            librarySyncService.status.collect { status ->
                _uiState.update { it.copy(isSyncing = status is SyncStatus.Syncing) }
                if (status is SyncStatus.Idle) {
                    loadFromRoom()
                } else if (status is SyncStatus.Error) {
                    _uiState.update { it.copy(error = status.message) }
                }
            }
        }
        viewModelScope.launch {
            loadFromRoom()
        }
    }

    private suspend fun loadFromRoom() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadLibraryEntriesUseCase()
            .onSuccess { entries ->
                Timber.i("ライブラリエントリーを取得: ${entries.size}件")
                val availableMedia = entries.mapNotNull { it.work.media }.distinct().sorted()
                val availableStatuses = entries.mapNotNull { it.statusState }.distinct()
                _uiState.update { currentState ->
                    currentState.copy(
                        allEntries = entries,
                        availableMedia = availableMedia,
                        availableStatuses = availableStatuses,
                        entries = applyFilters(entries, currentState.filterState),
                        isLoading = false,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                Timber.e(e, "ライブラリエントリーの読み込みに失敗")
                _uiState.update { it.copy(isLoading = false, error = errorMapper.toUserMessage(e)) }
            }
    }

    fun onEntryUpdated(libraryEntryId: String) {
        viewModelScope.launch {
            librarySyncService.syncEntry(libraryEntryId)
            loadFromRoom()
        }
    }

    fun toggleMediaFilter(media: String) {
        _uiState.update { currentState ->
            val newSelected = currentState.filterState.selectedMedia.let {
                if (media in it) it - media else it + media
            }
            val newFilterState = currentState.filterState.copy(selectedMedia = newSelected)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, newFilterState)
            )
        }
    }

    fun toggleStatusFilter(status: StatusState) {
        _uiState.update { currentState ->
            val newSelected = currentState.filterState.selectedStatuses.let {
                if (status in it) it - status else it + status
            }
            val newFilterState = currentState.filterState.copy(selectedStatuses = newSelected)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, newFilterState)
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            val newFilterState = currentState.filterState.copy(searchQuery = query)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, newFilterState)
            )
        }
    }

    fun toggleFilterVisibility() {
        _uiState.update { it.copy(isFilterVisible = !it.isFilterVisible) }
    }

    fun showDetail(entry: LibraryEntry) {
        Timber.i("DetailModalを表示: ${entry.work.title}")
        _uiState.update { it.copy(selectedEntry = entry, isDetailModalVisible = true) }
    }

    fun hideDetail() {
        Timber.i("DetailModalを非表示")
        _uiState.update { it.copy(selectedEntry = null, isDetailModalVisible = false) }
    }

    private fun applyFilters(entries: List<LibraryEntry>, filterState: LibraryFilterState): List<LibraryEntry> = entries
        .let { list ->
            if (filterState.selectedMedia.isEmpty()) {
                list
            } else {
                list.filter { it.work.media in filterState.selectedMedia }
            }
        }
        .let { list ->
            if (filterState.selectedStatuses.isEmpty()) {
                list
            } else {
                list.filter { it.statusState in filterState.selectedStatuses }
            }
        }
        .let { list ->
            if (filterState.searchQuery.isBlank()) {
                list
            } else {
                val query = filterState.searchQuery.trim().lowercase()
                list.filter { it.work.title.lowercase().contains(query) }
            }
        }
}
