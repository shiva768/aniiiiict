package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
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

private const val SEASON_SPRING = 0
private const val SEASON_SUMMER = 1
private const val SEASON_AUTUMN = 2
private const val SEASON_WINTER = 3

private val SEASON_ORDER = mapOf(
    SeasonName.SPRING to SEASON_SPRING,
    SeasonName.SUMMER to SEASON_SUMMER,
    SeasonName.AUTUMN to SEASON_AUTUMN,
    SeasonName.WINTER to SEASON_WINTER
)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

enum class LibrarySortOrder {
    TITLE_ASC,
    SEASON_DESC,
    SEASON_ASC
}

data class LibraryFilterState(
    val selectedMedia: Set<String> = emptySet(),
    val selectedStatuses: Set<StatusState> = emptySet(),
    val selectedYear: Int? = null,
    val selectedSeasons: Set<SeasonName> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.SEASON_DESC
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
    val availableYears: List<Int> = emptyList(),
    val availableSeasons: List<SeasonName> = emptyList(),
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
                val availableYears = entries.mapNotNull { it.work.seasonYear }.distinct().sortedDescending()
                val availableSeasons = entries.mapNotNull { it.work.seasonName }.distinct()
                    .sortedBy { SEASON_ORDER[it] }
                _uiState.update { currentState ->
                    currentState.copy(
                        allEntries = entries,
                        availableMedia = availableMedia,
                        availableStatuses = availableStatuses,
                        availableYears = availableYears,
                        availableSeasons = availableSeasons,
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

    fun toggleMediaFilter(media: String) = updateFilter { it.copy(selectedMedia = it.selectedMedia.toggle(media)) }

    fun toggleStatusFilter(status: StatusState) =
        updateFilter { it.copy(selectedStatuses = it.selectedStatuses.toggle(status)) }

    fun selectYear(year: Int?) = updateFilter { it.copy(selectedYear = year) }

    fun toggleSeasonFilter(season: SeasonName) =
        updateFilter { it.copy(selectedSeasons = it.selectedSeasons.toggle(season)) }

    fun updateSearchQuery(query: String) = updateFilter { it.copy(searchQuery = query) }

    fun updateSortOrder(sortOrder: LibrarySortOrder) = updateFilter { it.copy(sortOrder = sortOrder) }

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

    private fun updateFilter(transform: (LibraryFilterState) -> LibraryFilterState) {
        _uiState.update { currentState ->
            val newFilterState = transform(currentState.filterState)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, newFilterState)
            )
        }
    }

    @Suppress("ComplexMethod")
    private fun applyFilters(entries: List<LibraryEntry>, filterState: LibraryFilterState): List<LibraryEntry> {
        var filtered = entries
        if (filterState.selectedMedia.isNotEmpty()) {
            filtered = filtered.filter { it.work.media in filterState.selectedMedia }
        }
        if (filterState.selectedStatuses.isNotEmpty()) {
            filtered = filtered.filter { it.statusState in filterState.selectedStatuses }
        }
        if (filterState.selectedYear != null) {
            filtered = filtered.filter { it.work.seasonYear == filterState.selectedYear }
        }
        if (filterState.selectedSeasons.isNotEmpty()) {
            filtered = filtered.filter { it.work.seasonName in filterState.selectedSeasons }
        }
        val searched = if (filterState.searchQuery.isBlank()) {
            filtered
        } else {
            val query = filterState.searchQuery.trim().lowercase()
            filtered.filter { it.work.title.lowercase().contains(query) }
        }
        return sortEntries(searched, filterState.sortOrder)
    }

    private fun sortEntries(entries: List<LibraryEntry>, sortOrder: LibrarySortOrder): List<LibraryEntry> =
        when (sortOrder) {
            LibrarySortOrder.TITLE_ASC -> entries.sortedBy { it.work.title }
            LibrarySortOrder.SEASON_DESC -> entries.sortedWith(
                compareByDescending<LibraryEntry> { it.work.seasonYear }
                    .thenByDescending { SEASON_ORDER[it.work.seasonName] }
            )
            LibrarySortOrder.SEASON_ASC -> entries.sortedWith(
                compareBy<LibraryEntry> { it.work.seasonYear }
                    .thenBy { SEASON_ORDER[it.work.seasonName] }
            )
        }
}
