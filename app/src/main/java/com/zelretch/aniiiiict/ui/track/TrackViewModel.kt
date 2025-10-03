package com.zelretch.aniiiiict.ui.track

import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val records: List<Record> = emptyList(),
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
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val filterProgramsUseCase: FilterProgramsUseCase,
    private val filterPreferences: FilterPreferences,
    private val judgeFinaleUseCase: JudgeFinaleUseCase
) : BaseViewModel(), TrackViewModelContract {

    companion object {
        private const val RECORDING_SUCCESS_MESSAGE_DURATION_MS = 2000L
    }

    private val _uiState = MutableStateFlow(TrackUiState())
    override val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true) }
            runCatching {
                watchEpisodeUseCase(episodeId, workId, currentStatus).getOrThrow()
            }.onSuccess {
                onRecordSuccess(episodeId, workId)
            }.onFailure { e ->
                val msg = e.message ?: ErrorHandler.getUserMessage(
                    ErrorHandler.analyzeError(e, "TrackViewModel.recordEpisode")
                )
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
        viewModelScope.launch {
            delay(RECORDING_SUCCESS_MESSAGE_DURATION_MS)
            if (_uiState.value.recordingSuccess == episodeId) {
                _uiState.update { it.copy(recordingSuccess = null) }
            }
        }
        handleFinaleJudgement(episodeId, workId)
        refresh()
    }

    private suspend fun handleFinaleJudgement(episodeId: String, workId: String) {
        val program = _uiState.value.allPrograms.find { it.work.id == workId }
        val currentEpisode = program?.programs?.find { it.episode.id == episodeId }

        if (program == null || currentEpisode?.episode?.number == null) {
            return
        }

        val episodeNumber = currentEpisode.episode.number
        val malAnimeId = program.work.malAnimeId ?: return

        val judgeResult = judgeFinaleUseCase(episodeNumber, malAnimeId.toInt())

        if (judgeResult.isFinale) {
            _uiState.update { currentState ->
                currentState.copy(
                    showFinaleConfirmationForWorkId = workId,
                    showFinaleConfirmationForEpisodeNumber = episodeNumber
                )
            }
        }
    }

    fun confirmWatchedStatus() {
        _uiState.value.showFinaleConfirmationForWorkId?.let { workId ->
            viewModelScope.launch {
                runCatching {
                    updateViewStateUseCase(workId, StatusState.WATCHED)
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            showFinaleConfirmationForWorkId = null,
                            showFinaleConfirmationForEpisodeNumber = null
                        )
                    }
                    // Refresh the programs list to show the updated status
                    refresh()
                }.onFailure { e ->
                    val msg = e.message ?: ErrorHandler.getUserMessage(
                        ErrorHandler.analyzeError(e, "TrackViewModel.confirmWatchedStatus")
                    )
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
        viewModelScope.launch {
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

    fun getProgramWithWork(workId: String): ProgramWithWork? = _uiState.value.allPrograms.find { it.work.id == workId }
}
