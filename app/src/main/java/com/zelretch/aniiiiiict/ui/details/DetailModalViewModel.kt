package com.zelretch.aniiiiiict.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.type.StatusState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailModalState(
    val programs: List<Program> = emptyList(),
    val showConfirmDialog: Boolean = false,
    val selectedEpisodeIndex: Int? = null,
    val selectedStatus: StatusState? = null,
    val isStatusChanging: Boolean = false,
    val statusChangeError: String? = null,
    val workId: String = ""
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
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        statusChangeError = "ステータスの更新に失敗しました: ${e.message}",
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
                _events.emit(DetailModalEvent.EpisodesRecorded)
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    fun bulkRecordEpisodes(episodeIds: List<String>, status: StatusState) {
        val workId = _state.value.workId
        viewModelScope.launch {
            try {
                bulkRecordEpisodesUseCase(episodeIds, workId, status)
                val currentPrograms = _state.value.programs
                val targetEpisodes = currentPrograms.filterIndexed { index, _ ->
                    index <= (_state.value.selectedEpisodeIndex ?: return@launch)
                }
                _state.update {
                    it.copy(
                        programs = currentPrograms - targetEpisodes.toSet(),
                        showConfirmDialog = false,
                        selectedEpisodeIndex = null
                    )
                }
                _events.emit(DetailModalEvent.BulkEpisodesRecorded)
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }
} 