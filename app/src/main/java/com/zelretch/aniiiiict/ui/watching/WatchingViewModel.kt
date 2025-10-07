package com.zelretch.aniiiiict.ui.watching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
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
 * 視聴中作品画面のUI状態
 */
data class WatchingUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val allEntries: List<LibraryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showOnlyPastWorks: Boolean = true, // デフォルトは過去作のみ
    val filterState: FilterState = FilterState(),
    val isFilterVisible: Boolean = false
)

/**
 * 視聴中作品画面のViewModel
 */
@HiltViewModel
class WatchingViewModel @Inject constructor(
    private val loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchingUiState())
    val uiState: StateFlow<WatchingUiState> = _uiState.asStateFlow()

    init {
        loadLibraryEntries()
    }

    fun loadLibraryEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

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
        }
    }

    fun refresh() {
        Timber.i("ライブラリエントリーを再読み込み")
        loadLibraryEntries()
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

    private fun applyFilters(entries: List<LibraryEntry>, showOnlyPastWorks: Boolean): List<LibraryEntry> {
        // TODO: 過去作のみフィルターを実装する際に、viewer.programs との照合が必要
        // 現時点では全作品を返す
        return entries
    }
}
