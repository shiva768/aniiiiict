package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ライブラリ画面のUI状態
 */
data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val allEntries: List<LibraryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showOnlyPastWorks: Boolean = true,
    val filterState: FilterState = FilterState(),
    val availableMedia: List<String> = emptyList(),
    val isFilterVisible: Boolean = false,
    val selectedEntry: LibraryEntry? = null,
    val isDetailModalVisible: Boolean = false
)

/**
 * ライブラリ画面のViewModel
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase,
    private val loadProgramsUseCase: LoadProgramsUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // 放送中の作品IDを保持
    private var currentlyAiringWorkIds: Set<String> = emptySet()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 先に放送中の番組データを読み込む
            loadProgramsUseCase()
                .onSuccess { programs ->
                    currentlyAiringWorkIds = programs.map { it.work.id }.toSet()
                    Timber.i("放送中の作品を取得: ${currentlyAiringWorkIds.size}件")
                }
                .onFailure { e ->
                    Timber.e(e, "番組データの読み込みに失敗")
                    // 番組データの読み込みに失敗してもライブラリエントリーは表示する
                }

            // ライブラリエントリーを読み込む
            loadLibraryEntriesUseCase(listOf(StatusState.WATCHING))
                .onSuccess { entries ->
                    Timber.i("ライブラリエントリーを取得: ${entries.size}件")
                    val availableMedia = entries.mapNotNull { it.work.media }.distinct().sorted()
                    _uiState.update { currentState ->
                        currentState.copy(
                            allEntries = entries,
                            availableMedia = availableMedia,
                            entries = applyFilters(entries, currentState.showOnlyPastWorks, currentState.filterState),
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "ライブラリエントリーの読み込みに失敗")
                    val errorMessage = errorMapper.toUserMessage(e)
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
        }
    }

    fun loadLibraryEntries() {
        loadData()
    }

    fun refresh() {
        Timber.i("データを再読み込み")
        loadData()
    }

    fun togglePastWorksFilter() {
        _uiState.update { currentState ->
            val newShowOnlyPastWorks = !currentState.showOnlyPastWorks
            currentState.copy(
                showOnlyPastWorks = newShowOnlyPastWorks,
                entries = applyFilters(currentState.allEntries, newShowOnlyPastWorks, currentState.filterState)
            )
        }
    }

    fun toggleMediaFilter(media: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.filterState.selectedMedia
            val newSelected = if (media in currentSelected) currentSelected - media else currentSelected + media
            val newFilterState = currentState.filterState.copy(selectedMedia = newSelected)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, currentState.showOnlyPastWorks, newFilterState)
            )
        }
    }

    fun toggleFilterVisibility() {
        _uiState.update { it.copy(isFilterVisible = !it.isFilterVisible) }
    }

    fun showDetail(entry: LibraryEntry) {
        Timber.i("DetailModalを表示: ${entry.work.title}")
        _uiState.update {
            it.copy(
                selectedEntry = entry,
                isDetailModalVisible = true
            )
        }
    }

    fun hideDetail() {
        Timber.i("DetailModalを非表示")
        _uiState.update {
            it.copy(
                selectedEntry = null,
                isDetailModalVisible = false
            )
        }
    }

    private fun applyFilters(
        entries: List<LibraryEntry>,
        showOnlyPastWorks: Boolean,
        filterState: FilterState
    ): List<LibraryEntry> {
        var result = if (showOnlyPastWorks) {
            entries.filter { entry -> entry.work.id !in currentlyAiringWorkIds }
        } else {
            entries
        }
        if (filterState.selectedMedia.isNotEmpty()) {
            result = result.filter { entry -> entry.work.media in filterState.selectedMedia }
        }
        return result
    }
}
