package com.zelretch.aniiiiict.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import com.zelretch.aniiiiict.ui.base.launchWithMinLoadingTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 放送スケジュール画面のUI状態
 */
data class TrackUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val records: List<Record> = emptyList(),
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
    val isLoadingDetail: Boolean = false,
    val showFinaleConfirmationForWorkId: String? = null,
    val showFinaleConfirmationForEpisodeNumber: Int? = null
)

/**
 * 放送スケジュール画面のViewModel
 *
 * Now in Android パターンへの移行:
 * - BaseViewModelを削除し、明示的なエラーハンドリング
 * - launchWithMinLoadingTimeで最小ローディング時間を保証
 * - ErrorMapperによるユーザー向けメッセージ変換
 *
 * Note: 複雑な状態管理（フィルタ、モーダル、フィナーレ判定など）があるため、
 * 現時点では従来のUiStateパターンを維持。
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class TrackViewModel @Inject constructor(
    private val loadProgramsUseCase: LoadProgramsUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val programFilterManager: ProgramFilterManager,
    private val judgeFinaleUseCase: JudgeFinaleUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel(), TrackViewModelContract {

    companion object {
        private const val RECORDING_SUCCESS_MESSAGE_DURATION_MS = 2000L
    }

    private val _uiState = MutableStateFlow(TrackUiState())
    override val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            programFilterManager.filterState.collect { savedFilterState ->
                if (_uiState.value.allPrograms.isEmpty()) {
                    _uiState.update { it.copy(filterState = savedFilterState) }
                    loadingPrograms()
                } else {
                    _uiState.update {
                        it.copy(
                            filterState = savedFilterState,
                            programs = programFilterManager.filterPrograms(it.allPrograms, savedFilterState)
                        )
                    }
                }
            }
        }
    }

    override fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadingPrograms() {
        launchWithMinLoadingTime {
            _uiState.update { it.copy(isLoading = true, error = null) }

            loadProgramsUseCase()
                .onSuccess { programs ->
                    _uiState.update { currentState ->
                        val availableFilters = programFilterManager.extractAvailableFilters(programs)
                        val filteredPrograms = programFilterManager.filterPrograms(programs, currentState.filterState)
                        currentState.copy(
                            programs = filteredPrograms,
                            availableMedia = availableFilters.media,
                            availableSeasons = availableFilters.seasons,
                            availableYears = availableFilters.years,
                            availableChannels = availableFilters.channels,
                            allPrograms = programs,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "TrackViewModel.loadingPrograms")
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                    Timber.e(e, "プログラム一覧の読み込みに失敗: $msg")
                }
        }
    }

    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true) }

            watchEpisodeUseCase(episodeId, workId, currentStatus)
                .onSuccess {
                    onRecordSuccess(episodeId, workId)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "TrackViewModel.recordEpisode")
                    _uiState.update { it.copy(error = msg, isRecording = false) }
                    Timber.e(e, "エピソード記録に失敗: $msg")
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
                updateViewStateUseCase(workId, StatusState.WATCHED)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                showFinaleConfirmationForWorkId = null,
                                showFinaleConfirmationForEpisodeNumber = null
                            )
                        }
                        // Refresh the programs list to show the updated status
                        refresh()
                    }
                    .onFailure { e ->
                        val msg = errorMapper.toUserMessage(e, "TrackViewModel.confirmWatchedStatus")
                        _uiState.update { it.copy(error = msg) }
                        Timber.e(e, "ステータス更新に失敗: $msg")
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
                programs = programFilterManager.filterPrograms(currentState.allPrograms, newFilterState)
            )
        }
        viewModelScope.launch {
            programFilterManager.updateFilterState(newFilterState)
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
