package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.datastore.LibraryPreferences
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryFetchParams
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val allEntries: List<LibraryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val fetchParams: LibraryFetchParams = LibraryFetchParams(
        selectedStates = listOf(StatusState.WANNA_WATCH, StatusState.ON_HOLD),
        seasonFromYear = LocalDate.now().year - 5,
        seasonFromName = SeasonName.SPRING
    ),
    val filterState: FilterState = FilterState(),
    val availableMedia: List<String> = emptyList(),
    val isFilterVisible: Boolean = false,
    val selectedEntry: LibraryEntry? = null,
    val isDetailModalVisible: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase,
    private val libraryPreferences: LibraryPreferences,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            libraryPreferences.fetchPrefs.collect { params ->
                _uiState.update { it.copy(fetchParams = params) }
                loadData(params)
            }
        }
    }

    private suspend fun loadData(params: LibraryFetchParams, forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadLibraryEntriesUseCase(params, forceRefresh)
            .onSuccess { entries ->
                Timber.i("ライブラリエントリーを取得: ${entries.size}件")
                val availableMedia = entries.mapNotNull { it.work.media }.distinct().sorted()
                _uiState.update { currentState ->
                    currentState.copy(
                        allEntries = entries,
                        availableMedia = availableMedia,
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

    fun refresh() {
        viewModelScope.launch {
            loadData(_uiState.value.fetchParams, forceRefresh = true)
        }
    }

    fun updateFetchParams(params: LibraryFetchParams) {
        viewModelScope.launch {
            libraryPreferences.updateFetchPrefs(params)
        }
    }

    fun toggleMediaFilter(media: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.filterState.selectedMedia
            val newSelected = if (media in currentSelected) currentSelected - media else currentSelected + media
            val newFilterState = currentState.filterState.copy(selectedMedia = newSelected)
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

    private fun applyFilters(entries: List<LibraryEntry>, filterState: FilterState): List<LibraryEntry> {
        if (filterState.selectedMedia.isEmpty()) return entries
        return entries.filter { it.work.media in filterState.selectedMedia }
    }
}
