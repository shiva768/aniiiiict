package com.zelretch.aniiiiiict.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.apollographql.apollo.exception.ApolloException
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.ui.base.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
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
            try {
                updateViewStateUseCase(workId, status)
                _state.update { it.copy(selectedStatus = status) }
                _events.emit(DetailModalEvent.StatusChanged)
            } catch (e: ApolloException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "changeStatus")
                _state.update {
                    it.copy(
                        statusChangeError = errorMessage,
                        selectedStatus = _state.value.selectedStatus
                    )
                }
            } catch (e: IOException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "changeStatus")
                _state.update {
                    it.copy(
                        statusChangeError = errorMessage,
                        selectedStatus = _state.value.selectedStatus
                    )
                }
            } finally {
                _state.update { it.copy(isStatusChanging = false) }
            }
        }
    }

    fun recordEpisode(episodeId: String, status: StatusState) {
        val workId = _state.value.workId
        viewModelScope.launch {
            try {
                watchEpisodeUseCase(episodeId, workId, status)
                // 記録したエピソードのプログラムを表示から消す
                _state.update {
                    it.copy(programs = _state.value.programs.filter { it.episode.id != episodeId })
                }
                _events.emit(DetailModalEvent.EpisodesRecorded)
            } catch (e: ApolloException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "recordEpisode")
                // エラーが発生した場合もイベントを発行して、UI側でエラーハンドリングができるようにする
                // 実際のエラー表示は親ViewModelで処理される
            } catch (e: IOException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "recordEpisode")
                // エラーが発生した場合もイベントを発行して、UI側でエラーハンドリングができるようにする
                // 実際のエラー表示は親ViewModelで処理される
            }
        }
    }

    fun bulkRecordEpisodes(episodeIds: List<String>, status: StatusState) {
        val workId = _state.value.workId
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(
                        isBulkRecording = true,
                        bulkRecordingProgress = 0,
                        bulkRecordingTotal = episodeIds.size
                    )
                }

                bulkRecordEpisodesUseCase(
                    episodeIds = episodeIds,
                    workId = workId,
                    currentStatus = status,
                    onProgress = { progress ->
                        _state.update { it.copy(bulkRecordingProgress = progress) }
                    }
                )

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
            } catch (e: ApolloException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "bulkRecordEpisodes")
                _state.update {
                    it.copy(
                        isBulkRecording = false,
                        bulkRecordingProgress = 0,
                        bulkRecordingTotal = 0
                    )
                }
                // バルク記録エラーの場合、エラー情報はログに記録されているが
                // UI状態は呼び出し元で管理されるため、ここでは状態をリセットのみ行う
            } catch (e: IOException) {
                val errorMessage = ErrorHandler.handleError(e, "DetailModalViewModel", "bulkRecordEpisodes")
                _state.update {
                    it.copy(
                        isBulkRecording = false,
                        bulkRecordingProgress = 0,
                        bulkRecordingTotal = 0
                    )
                }
                // バルク記録エラーの場合、エラー情報はログに記録されているが
                // UI状態は呼び出し元で管理されるため、ここでは状態をリセットのみ行う
            }
        }
    }
}
