package com.zelretch.aniiiiiict.ui.track

import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import com.zelretch.aniiiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TrackUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val records: List<com.zelretch.aniiiiiict.data.model.Record> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
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
    val isLoadingDetail: Boolean = false,
    val showFinaleConfirmationForWorkId: String? = null,
    val showFinaleConfirmationForEpisodeNumber: Int? = null
) : BaseUiState(isLoading, error)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val loadProgramsUseCase: LoadProgramsUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val filterProgramsUseCase: FilterProgramsUseCase,
    private val filterPreferences: FilterPreferences,
    private val judgeFinaleUseCase: JudgeFinaleUseCase
) : BaseViewModel(), TrackViewModelContract, TestableTrackViewModel {
    private val _uiState = MutableStateFlow(TrackUiState())
    override val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    /**
     * テスト用: コルーチンスコープ差し替え用
     */
    override var externalScope: CoroutineScope? = null

    init {
        println("[TrackViewModel] init: start collecting filterPreferences.filterState")
        (externalScope ?: viewModelScope).launch {
            filterPreferences.filterState.collect { savedFilterState ->
                println("[TrackViewModel] collect: $savedFilterState")
                if (_uiState.value.allPrograms.isEmpty()) {
                    _uiState.update { currentState ->
                        currentState.copy(filterState = savedFilterState)
                    }
                    loadingPrograms()
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            filterState = savedFilterState,
                            programs = filterProgramsUseCase(
                                currentState.allPrograms,
                                savedFilterState
                            )
                        )
                    }
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

    override fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadingPrograms() {
        executeWithLoading {
            loadProgramsUseCase.invoke().collect { programs ->
                _uiState.update { currentState ->
                    val availableFilters = filterProgramsUseCase.extractAvailableFilters(programs)
                    val filteredPrograms = filterProgramsUseCase(programs, currentState.filterState)
                    currentState.copy(
                        programs = filteredPrograms,
                        availableMedia = availableFilters.media,
                        availableSeasons = availableFilters.seasons,
                        availableYears = availableFilters.years,
                        availableChannels = availableFilters.channels,
                        allPrograms = programs,
                        error = null
                    )
                }
            }
        }
    }

    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        (externalScope ?: viewModelScope).launch {
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

                    // AniListから作品情報を取得し、最終話判定を行う
                    val program = _uiState.value.allPrograms.find { it.work.id == workId }
                    Timber.i(
                        "TrackViewModel",
                        "[DEBUG_LOG] allPrograms size: ${_uiState.value.allPrograms.size}",
                        "TrackViewModel.recordEpisode"
                    )
                    Timber.i(
                        "TrackViewModel",
                        "[DEBUG_LOG] program: $program",
                        "TrackViewModel.recordEpisode"
                    )

                    val currentEpisode = program?.programs?.find { it.episode.id == episodeId }
                    Timber.i(
                        "TrackViewModel",
                        "[DEBUG_LOG] currentEpisode: $currentEpisode",
                        "TrackViewModel.recordEpisode"
                    )

                    if (program != null &&
                        currentEpisode != null &&
                        currentEpisode.episode.number != null
                    ) {
                        Timber.i(
                            "TrackViewModel",
                            "[DEBUG_LOG] episode number: ${currentEpisode.episode.number}",
                            "TrackViewModel.recordEpisode"
                        )
                        val judgeResult = judgeFinaleUseCase(
                            currentEpisode.episode.number,
                            program.work.id.toInt()
                        )
                        Timber.i(
                            "TrackViewModel",
                            "[DEBUG_LOG] judgeResult: $judgeResult",
                            "TrackViewModel.recordEpisode"
                        )
                        if (judgeResult.isFinale) {
                            Timber.i(
                                "TrackViewModel",
                                "[DEBUG_LOG] Setting showFinaleConfirmationForWorkId to $workId",
                                "TrackViewModel.recordEpisode"
                            )
                            _uiState.update {
                                it.copy(
                                    showFinaleConfirmationForWorkId = workId,
                                    showFinaleConfirmationForEpisodeNumber = currentEpisode.episode.number
                                )
                            }
                        } else {
                            Timber.i(
                                "TrackViewModel",
                                "[DEBUG_LOG] judgeResult.isFinale is false",
                                "TrackViewModel.recordEpisode"
                            )
                        }
                    } else {
                        Timber.i(
                            "TrackViewModel",
                            "[DEBUG_LOG] program or currentEpisode is null, or episode number is null",
                            "TrackViewModel.recordEpisode"
                        )
                    }
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

    fun confirmWatchedStatus() {
        _uiState.value.showFinaleConfirmationForWorkId?.let { workId ->
            (externalScope ?: viewModelScope).launch {
                updateViewState(workId, StatusState.WATCHED)
                _uiState.update {
                    it.copy(
                        showFinaleConfirmationForWorkId = null,
                        showFinaleConfirmationForEpisodeNumber = null
                    )
                }
            }
        }
    }

    fun dismissFinaleConfirmation() {
        _uiState.update {
            it.copy(
                showFinaleConfirmationForWorkId = null,
                showFinaleConfirmationForEpisodeNumber = null
            )
        }
    }

    fun updateViewState(workId: String, status: StatusState) {
        (externalScope ?: viewModelScope).launch {
            try {
                // Use a dummy episodeId since we're only updating the status
                // Set shouldUpdateStatus to true to force status update
                runCatching {
                    watchEpisodeUseCase("", workId, status, true).getOrThrow()
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            error = null
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "ステータスの更新に失敗しました"
                        )
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun refresh() {
        Timber.i("TrackViewModel", "プログラム一覧を再読み込み", "TrackViewModel.refresh")
        loadingPrograms()
    }

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
                programs = filterProgramsUseCase(currentState.allPrograms, newFilterState)
            )
        }
        (externalScope ?: viewModelScope).launch {
            filterPreferences.updateFilterState(_uiState.value.filterState)
        }
    }

    override fun toggleFilterVisibility() {
        _uiState.update {
            it.copy(
                isFilterVisible = !it.isFilterVisible
            )
        }
    }

    private fun handleError(error: Throwable) {
        Timber.e("TrackViewModel", error, "TrackViewModel")
        _uiState.update { it.copy(error = error.message) }
    }

    fun showUnwatchedEpisodes(program: ProgramWithWork) {
        _uiState.update {
            it.copy(
                selectedProgram = program,
                isDetailModalVisible = true,
                isLoadingDetail = false
            )
        }
    }

    fun hideDetail() {
        _uiState.update {
            it.copy(
                isDetailModalVisible = false,
                selectedProgram = null,
                isLoadingDetail = false
            )
        }
    }

    // === TrackViewModelContract interface implementation ===

    override fun watchEpisode(program: ProgramWithWork, episodeNumber: Int) {
        val episode = program.programs.find { it.episode.number == episodeNumber }
        episode?.let {
            recordEpisode(it.episode.id, program.work.id, program.work.viewerStatusState)
        }
    }

    override fun showDetailModal(program: ProgramWithWork) {
        showUnwatchedEpisodes(program)
    }

    override fun hideDetailModal() {
        hideDetail()
    }

    override fun showFinaleConfirmation(workId: String, episodeNumber: Int) {
        _uiState.update {
            it.copy(
                showFinaleConfirmationForWorkId = workId,
                showFinaleConfirmationForEpisodeNumber = episodeNumber
            )
        }
    }

    override fun hideFinaleConfirmation() {
        dismissFinaleConfirmation()
    }

    override fun recordFinale(workId: String, episodeNumber: Int) {
        confirmWatchedStatus()
    }

    // === TestableTrackViewModel interface implementation ===

    override fun setUiStateForTest(state: TrackUiState) {
        _uiState.value = state
    }
}
