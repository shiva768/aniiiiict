package com.zelretch.aniiiiict.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
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
    val bulkRecordingTotal: Int = 0
)

sealed interface DetailModalEvent {
    object StatusChanged : DetailModalEvent
    object EpisodesRecorded : DetailModalEvent
    object BulkEpisodesRecorded : DetailModalEvent
}

@HiltViewModel
class DetailModalViewModel @Inject constructor(
    private val bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase
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
                workId = programWithWork.work.id
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
        _state.update { it.copy(isStatusChanging = true, statusChangeError = null) }
        viewModelScope.launch {
            runCatching {
                updateViewStateUseCase(workId, status).getOrThrow()
            }.onSuccess {
                _state.update { it.copy(selectedStatus = status) }
                _events.emit(DetailModalEvent.StatusChanged)
            }.onFailure { e ->
                val errorMessage = e.message ?: ErrorHandler.getUserMessage(
                    ErrorHandler.analyzeError(e, "DetailModalViewModel.changeStatus")
                )
                _state.update {
                    it.copy(
                        statusChangeError = errorMessage,
                        selectedStatus = _state.value.selectedStatus
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
}
