package com.zelretch.aniiiiiict.ui.track

import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.ui.base.ErrorHandler
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

@Suppress("TooManyFunctions")
@HiltViewModel
class TrackViewModel @Inject constructor(
    private val loadProgramsUseCase: LoadProgramsUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val filterProgramsUseCase: FilterProgramsUseCase,
    private val filterPreferences: FilterPreferences,
    private val judgeFinaleUseCase: JudgeFinaleUseCase
) : BaseViewModel(), TrackViewModelContract, TestableTrackViewModel {

    companion object {
        private const val RECORDING_SUCCESS_MESSAGE_DURATION_MS = 2000L
    }

    private val _uiState = MutableStateFlow(TrackUiState())
    override val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    override var externalScope: CoroutineScope? = null

    init {
        (externalScope ?: viewModelScope).launch {
            filterPreferences.filterState.collect { savedFilterState ->
                if (_uiState.value.allPrograms.isEmpty()) {
                    _uiState.update { it.copy(filterState = savedFilterState) }
                    loadingPrograms()
                } else {
                    _uiState.update {
                        it.copy(
                            filterState = savedFilterState,
                            programs = filterProgramsUseCase(it.allPrograms, savedFilterState)
                        )
                    }
                }
            }
        }
    }

    // region BaseViewModel
    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    override fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    // endregion

    private fun loadingPrograms() = executeWithLoading {
        loadProgramsUseCase().collect { programs ->
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

    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        (externalScope ?: viewModelScope).launch {
            _uiState.update { it.copy(isRecording = true) }
            runCatching {
                watchEpisodeUseCase(episodeId, workId, currentStatus).getOrThrow()
            }.onSuccess {
                onRecordSuccess(episodeId, workId)
            }.onFailure { e ->
                val msg = ErrorHandler.handleError(e, "TrackViewModel", "recordEpisode")
                _uiState.update { it.copy(error = msg, isRecording = false) }
            }
        }
    }

    private suspend fun onRecordSuccess(episodeId: String, workId: String) {
        _uiState.update {
            it.copy(
                isRecording = false,
                recordingSuccess = episodeId,
                error = null
            )
        }
        (externalScope ?: viewModelScope).launch {
            delay(RECORDING_SUCCESS_MESSAGE_DURATION_MS)
            if (_uiState.value.recordingSuccess == episodeId) {
                _uiState.update { it.copy(recordingSuccess = null) }
            }
        }
        // プログラムの再読み込みが完了してから最終話判定を行う
        val reloadJob = loadingPrograms()
        reloadJob.join()
        handleFinaleJudgement(episodeId, workId)
    }

    private fun onRecordFailure(e: Throwable) {
        _uiState.update {
            it.copy(
                isRecording = false,
                error = e.message ?: "エピソードの記録に失敗しました"
            )
        }
    }

    private suspend fun handleFinaleJudgement(episodeId: String, workId: String) {
        val program = _uiState.value.allPrograms.find { it.work.id == workId }
        val currentEpisode = program?.programs?.find { it.episode.id == episodeId }

        if (program != null && currentEpisode?.episode?.number != null) {
            val judgeResult = judgeFinaleUseCase(
                currentEpisode.episode.number,
                program.work.id.toInt()
            )
            if (judgeResult.isFinale) {
                _uiState.update {
                    it.copy(
                        showFinaleConfirmationForWorkId = workId,
                        showFinaleConfirmationForEpisodeNumber = currentEpisode.episode.number
                    )
                }
            }
        }
    }

    fun confirmWatchedStatus() {
        _uiState.value.showFinaleConfirmationForWorkId?.let { workId ->
            (externalScope ?: viewModelScope).launch {
                runCatching {
                    watchEpisodeUseCase("", workId, StatusState.WATCHED, true).getOrThrow()
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            showFinaleConfirmationForWorkId = null,
                            showFinaleConfirmationForEpisodeNumber = null
                        )
                    }
                }.onFailure { e ->
                    val msg = ErrorHandler.handleError(e, "TrackViewModel", "confirmWatchedStatus")
                    _uiState.update { it.copy(error = msg) }
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
            runCatching {
                watchEpisodeUseCase("", workId, status, true).getOrThrow()
            }.onSuccess {
                _uiState.update { it.copy(error = null) }
            }.onFailure { e ->
                val msg = ErrorHandler.handleError(e, "TrackViewModel", "updateViewState")
                _uiState.update { it.copy(error = msg) }
            }
        }
    }

    fun refresh() {
        Timber.i("プログラム一覧を再読み込み")
        loadingPrograms()
    }

    fun updateFilter(newFilterState: FilterState) {
        _uiState.update { currentState ->
            currentState.copy(
                filterState = newFilterState,
                programs = filterProgramsUseCase(currentState.allPrograms, newFilterState)
            )
        }
        (externalScope ?: viewModelScope).launch {
            filterPreferences.updateFilterState(newFilterState)
        }
    }

    override fun toggleFilterVisibility() {
        _uiState.update { it.copy(isFilterVisible = !it.isFilterVisible) }
    }

    override fun watchEpisode(program: ProgramWithWork, episodeNumber: Int) {
        val episode = program.programs.find { it.episode.number == episodeNumber }
        episode?.let {
            recordEpisode(it.episode.id, program.work.id, program.work.viewerStatusState)
        }
    }

    // region TestableTrackViewModel
    override fun setUiStateForTest(state: TrackUiState) {
        _uiState.value = state
    }
    // endregion

    override fun showDetailModal(program: ProgramWithWork) {
        showUnwatchedEpisodes(program)
    }

    override fun hideDetailModal() {
        hideDetail()
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
}
