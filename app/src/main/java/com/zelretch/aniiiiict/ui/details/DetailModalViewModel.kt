package com.zelretch.aniiiiict.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailModalState(
    val programs: List<Program> = emptyList(),
    val showConfirmDialog: Boolean = false,
    val selectedEpisodeIndex: Int? = null,
    val selectedStatus: StatusState? = null,
    val isStatusChanging: Boolean = false,
    val statusChangeError: String? = null,
    val workId: String = "",
    val isBulkRecording: Boolean = false,
    val bulkRecordingProgress: Int = 0,
    val bulkRecordingTotal: Int = 0,
    val showFinaleConfirmationForWorkId: String? = null,
    val showFinaleConfirmationForEpisodeNumber: Int? = null,
    val work: Work? = null
)

sealed interface DetailModalEvent {
    object StatusChanged : DetailModalEvent
    object EpisodesRecorded : DetailModalEvent
    object BulkEpisodesRecorded : DetailModalEvent
    data class ShowFinaleConfirmation(val workId: String, val episodeNumber: Int) : DetailModalEvent
}

@HiltViewModel
class DetailModalViewModel @Inject constructor(
    private val bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val judgeFinaleUseCase: JudgeFinaleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DetailModalState())
    val state: StateFlow<DetailModalState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DetailModalEvent>()
    val events: SharedFlow<DetailModalEvent> = _events.asSharedFlow()

    fun initialize(programWithWork: ProgramWithWork) {
        _state.update {
            it.copy(
                programs = programWithWork.programs,
                selectedStatus = programWithWork.work.viewerStatusState,
                workId = programWithWork.work.id,
                work = programWithWork.work
            )
        }
    }

    fun showConfirmDialog(index: Int) {
        _state.update { it.copy(showConfirmDialog = true, selectedEpisodeIndex = index) }
    }

    fun hideConfirmDialog() {
        _state.update { it.copy(showConfirmDialog = false, selectedEpisodeIndex = null) }
    }

    fun changeStatus(status: StatusState) {
        val workId = _state.value.workId
        val previous = _state.value.selectedStatus
        // Optimistically update UI so tests can immediately see the new value
        _state.update { it.copy(isStatusChanging = true, statusChangeError = null, selectedStatus = status) }
        viewModelScope.launch {
            runCatching {
                updateViewStateUseCase(workId, status).getOrThrow()
            }.onSuccess {
                // Keep the selected status (already set) and notify
                _events.emit(DetailModalEvent.StatusChanged)
            }.onFailure { e ->
                val errorMessage = e.message ?: ErrorHandler.getUserMessage(
                    ErrorHandler.analyzeError(e, "DetailModalViewModel.changeStatus")
                )
                // Roll back to previous status and show error
                _state.update {
                    it.copy(
                        statusChangeError = errorMessage,
                        selectedStatus = previous
                    )
                }
            }
            _state.update { it.copy(isStatusChanging = false) }
        }
    }

    fun recordEpisode(episodeId: String, status: StatusState) {
        val workId = _state.value.workId
        viewModelScope.launch {
            runCatching {
                watchEpisodeUseCase(episodeId, workId, status).getOrThrow()
            }.onSuccess {
                // 記録したエピソードのプログラムを表示から消す
                _state.update {
                    it.copy(programs = _state.value.programs.filter { it.episode.id != episodeId })
                }
                _events.emit(DetailModalEvent.EpisodesRecorded)
            }.onFailure { e ->
                // 表示は親で処理する設計のため、ここではユーザ向け文言を整形してログ化のみ
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.recordEpisode")
                ErrorHandler.logError("DetailModalViewModel", "recordEpisode", info)
            }
        }
    }

    fun bulkRecordEpisodes(episodeIds: List<String>, status: StatusState) {
        val workId = _state.value.workId
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBulkRecording = true,
                    bulkRecordingProgress = 0,
                    bulkRecordingTotal = episodeIds.size
                )
            }

            runCatching {
                bulkRecordEpisodesUseCase(
                    episodeIds = episodeIds,
                    workId = workId,
                    currentStatus = status,
                    onProgress = { progress ->
                        _state.update { it.copy(bulkRecordingProgress = progress) }
                    }
                ).getOrThrow()
            }.onSuccess {
                // Handle finale detection for the last recorded episode BEFORE removing programs
                handleFinaleDetectionAfterBulk(episodeIds.last())

                val currentPrograms = _state.value.programs
                val targetPrograms = currentPrograms.filterIndexed { index, _ ->
                    index <= (_state.value.selectedEpisodeIndex ?: return@launch)
                }
                _state.update {
                    it.copy(
                        programs = currentPrograms - targetPrograms.toSet(),
                        showConfirmDialog = false,
                        selectedEpisodeIndex = null,
                        isBulkRecording = false,
                        bulkRecordingProgress = 0,
                        bulkRecordingTotal = 0
                    )
                }
                _events.emit(DetailModalEvent.BulkEpisodesRecorded)
            }.onFailure { e ->
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.bulkRecordEpisodes")
                ErrorHandler.logError("DetailModalViewModel", "bulkRecordEpisodes", info)
                _state.update {
                    it.copy(
                        isBulkRecording = false,
                        bulkRecordingProgress = 0,
                        bulkRecordingTotal = 0
                    )
                }
            }
        }
    }

    private suspend fun handleFinaleDetectionAfterBulk(lastEpisodeId: String) {
        val work = _state.value.work ?: return
        val lastEpisode = _state.value.programs.find { it.episode.id == lastEpisodeId } ?: return

        val episodeNumber = lastEpisode.episode.number ?: return
        val malAnimeId = work.malAnimeId ?: return

        val judgeResult = judgeFinaleUseCase(episodeNumber, malAnimeId.toInt())

        if (judgeResult.isFinale) {
            _state.update { currentState ->
                currentState.copy(
                    showFinaleConfirmationForWorkId = work.id,
                    showFinaleConfirmationForEpisodeNumber = episodeNumber
                )
            }
            _events.emit(DetailModalEvent.ShowFinaleConfirmation(work.id, episodeNumber))
        }
    }

    fun hideFinaleConfirmation() {
        _state.update {
            it.copy(
                showFinaleConfirmationForWorkId = null,
                showFinaleConfirmationForEpisodeNumber = null
            )
        }
    }

    fun confirmFinaleWatchedStatus() {
        val workId = _state.value.showFinaleConfirmationForWorkId ?: return
        viewModelScope.launch {
            runCatching {
                updateViewStateUseCase(workId, StatusState.WATCHED)
            }.onSuccess {
                _state.update {
                    it.copy(
                        showFinaleConfirmationForWorkId = null,
                        showFinaleConfirmationForEpisodeNumber = null
                    )
                }
                _events.emit(DetailModalEvent.StatusChanged)
            }.onFailure { e ->
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.confirmFinaleWatchedStatus")
                ErrorHandler.logError("DetailModalViewModel", "confirmFinaleWatchedStatus", info)
            }
        }
    }
}
