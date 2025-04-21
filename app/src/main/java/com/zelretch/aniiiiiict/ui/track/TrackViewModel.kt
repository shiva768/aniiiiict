package com.zelretch.aniiiiiict.ui.track

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val records: List<com.zelretch.aniiiiiict.data.model.Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val recordingSuccess: String? = null,
    val filterState: FilterState = FilterState(),
    val isFilterVisible: Boolean = false,
    val availableMedia: List<String> = emptyList(),
    val availableSeasons: List<SeasonName> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableChannels: List<String> = emptyList(),
    val allPrograms: List<ProgramWithWork> = emptyList(),
    val selectedProgram: ProgramWithWork? = null,
    val isDetailModalVisible: Boolean = false,
    val isLoadingDetail: Boolean = false
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repository: AnnictRepository,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val programFilter: ProgramFilter,
    private val filterPreferences: FilterPreferences,
    logger: Logger,
    @ApplicationContext private val context: Context
) : BaseViewModel(logger) {
    private val TAG = "TrackViewModel"

    // UI状態のカプセル化
    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // フィルター状態の復元を待ってから認証状態のチェックを行う
            filterPreferences.filterState.collect { savedFilterState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        filterState = savedFilterState,
                        programs = programFilter.applyFilters(
                            currentState.allPrograms,
                            savedFilterState
                        )
                    )
                }
            }
        }
    }

    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    // 番組一覧の読み込み（内部メソッド）
    private fun loadingPrograms() {
        executeWithLoading {
            loadPrograms()
        }
    }

    // 番組一覧の読み込み（内部メソッド）
    private suspend fun loadPrograms() {
        repository.getProgramsWithWorks()
            .collect { programs ->
                _uiState.update { currentState ->
                    val availableFilters = programFilter.extractAvailableFilters(programs)
                    val filteredPrograms =
                        programFilter.applyFilters(programs, currentState.filterState)

                    currentState.copy(
                        programs = filteredPrograms,
                        availableMedia = availableFilters.media,
                        availableSeasons = availableFilters.seasons,
                        availableYears = availableFilters.years,
                        availableChannels = availableFilters.channels,
                        allPrograms = programs
                    )
                }
            }
    }

    // エピソードの記録（公開メソッド）
    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }
                runCatching {
                    watchEpisodeUseCase(episodeId, workId, currentStatus).getOrThrow()
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            recordingSuccess = episodeId,
                            error = null
                        )
                    }

                    delay(2000)
                    if (_uiState.value.recordingSuccess == episodeId) {
                        _uiState.update { it.copy(recordingSuccess = null) }
                    }

                    loadingPrograms()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            error = e.message ?: "エピソードの記録に失敗しました"
                        )
                    }
                }
            } catch (e: Exception) {
                handleError(e)
                _uiState.update { it.copy(isRecording = false) }
            }
        }
    }

    // エピソードの一括記録（公開メソッド）
    fun bulkRecordEpisode(episodeIds: List<String>, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }
                runCatching {
                    episodeIds.forEach { id ->
                        watchEpisodeUseCase(id, workId, currentStatus).getOrThrow()
                    }
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            recordingSuccess = episodeIds.lastOrNull(),
                            error = null
                        )
                    }

                    delay(2000)
                    if (_uiState.value.recordingSuccess == episodeIds.lastOrNull()) {
                        _uiState.update { it.copy(recordingSuccess = null) }
                    }

                    loadingPrograms()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            error = e.message ?: "エピソードの記録に失敗しました"
                        )
                    }
                }
            } catch (e: Exception) {
                handleError(e)
                _uiState.update { it.copy(isRecording = false) }
            }
        }
    }

    // 再読み込み（公開メソッド）
    fun refresh() {
        logger.info(TAG, "プログラム一覧を再読み込み", "TrackViewModel.refresh")
        loadingPrograms()
    }

    // フィルターの更新（公開メソッド）
    fun updateFilter(
        selectedMedia: Set<String> = _uiState.value.filterState.selectedMedia,
        selectedSeason: Set<SeasonName> = _uiState.value.filterState.selectedSeason,
        selectedYear: Set<Int> = _uiState.value.filterState.selectedYear,
        selectedChannel: Set<String> = _uiState.value.filterState.selectedChannel,
        selectedStatus: Set<StatusState> = _uiState.value.filterState.selectedStatus,
        searchQuery: String = _uiState.value.filterState.searchQuery,
        showOnlyAired: Boolean = _uiState.value.filterState.showOnlyAired,
        sortOrder: SortOrder = _uiState.value.filterState.sortOrder
    ) {
        _uiState.update { currentState ->
            val newFilterState = currentState.filterState.copy(
                selectedMedia = selectedMedia,
                selectedSeason = selectedSeason,
                selectedYear = selectedYear,
                selectedChannel = selectedChannel,
                selectedStatus = selectedStatus,
                searchQuery = searchQuery,
                showOnlyAired = showOnlyAired,
                sortOrder = sortOrder
            )
            currentState.copy(
                filterState = newFilterState,
                programs = programFilter.applyFilters(currentState.allPrograms, newFilterState)
            )
        }
        // フィルター状態の保存
        viewModelScope.launch {
            filterPreferences.updateFilterState(_uiState.value.filterState)
        }
    }

    // フィルターの表示/非表示の切り替え（公開メソッド）
    fun toggleFilterVisibility() {
        _uiState.update {
            it.copy(
                isFilterVisible = !it.isFilterVisible
            )
        }
    }

    // エラー処理（内部メソッド）
    private fun handleError(error: Throwable) {
        logger.error(TAG, error, "TrackViewModel")
        _uiState.update { it.copy(error = error.message) }
    }

    // 未視聴エピソードモーダルを表示
    fun showUnwatchedEpisodes(program: ProgramWithWork) {
        _uiState.update {
            it.copy(
                selectedProgram = program,
                isDetailModalVisible = true,
                isLoadingDetail = false
            )
        }
    }

    // 未視聴エピソードモーダルを非表示
    fun hideDetail() {
        _uiState.update {
            it.copy(
                isDetailModalVisible = false,
                selectedProgram = null,
                isLoadingDetail = false
            )
        }
    }
} 