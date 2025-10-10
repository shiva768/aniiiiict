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
import kotlinx.coroutines.flow.catch
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
    val showOnlyPastWorks: Boolean = true, // デフォルトは過去作のみ
    val filterState: FilterState = FilterState(),
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

            try {
                // 先に放送中の番組データを読み込む
                loadProgramsUseCase()
                    .catch { e ->
                        Timber.e(e, "番組データの読み込みに失敗")
                        // 番組データの読み込みに失敗してもライブラリエントリーは表示する
                    }
                    .collect { programs ->
                        currentlyAiringWorkIds = programs.map { it.work.id }.toSet()
                        Timber.i("放送中の作品を取得: ${currentlyAiringWorkIds.size}件")
                    }

                // ライブラリエントリーを読み込む
                loadLibraryEntriesUseCase(listOf(StatusState.WATCHING))
                    .catch { e ->
                        Timber.e(e, "ライブラリエントリーの読み込みに失敗")
                        val errorMessage = errorMapper.toUserMessage(e)
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                    .collect { entries ->
                        Timber.i("ライブラリエントリーを取得: ${entries.size}件")
                        _uiState.update { currentState ->
                            currentState.copy(
                                allEntries = entries,
                                entries = applyFilters(entries, currentState.showOnlyPastWorks),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "データの読み込みに失敗")
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
                entries = applyFilters(currentState.allEntries, newShowOnlyPastWorks)
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

    private fun applyFilters(entries: List<LibraryEntry>, showOnlyPastWorks: Boolean): List<LibraryEntry> =
        if (showOnlyPastWorks) {
            // 過去作のみ: 放送中の番組リストに存在しない作品
            entries.filter { entry ->
                entry.work.id !in currentlyAiringWorkIds
            }
        } else {
            // 全作品
            entries
        }
}
