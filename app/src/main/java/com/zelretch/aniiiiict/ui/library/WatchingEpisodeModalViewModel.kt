package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WatchingEpisodeModalState(
    val episode: Episode? = null,
    val selectedStatus: StatusState? = null,
    val isStatusChanging: Boolean = false,
    val statusChangeError: String? = null,
    val workId: String = "",
    val workTitle: String = "",
    val noEpisodes: Boolean = false
)

sealed interface WatchingEpisodeModalEvent {
    object StatusChanged : WatchingEpisodeModalEvent
    object EpisodeRecorded : WatchingEpisodeModalEvent
}

/**
 * WatchingEpisodeModal画面のViewModel
 * Library画面でライブラリエントリからエピソードを記録するために使用
 *
 * Note: BroadcastEpisodeModalと異なり、単一エピソード記録のみをサポート
 * （ライブラリには放送スケジュールがないため）
 */
@HiltViewModel
class WatchingEpisodeModalViewModel @Inject constructor(
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _state = MutableStateFlow(WatchingEpisodeModalState())
    val state: StateFlow<WatchingEpisodeModalState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<WatchingEpisodeModalEvent>()
    val events: SharedFlow<WatchingEpisodeModalEvent> = _events.asSharedFlow()

    fun initialize(entry: LibraryEntry) {
        _state.update {
            it.copy(
                episode = entry.nextEpisode,
                selectedStatus = entry.work.viewerStatusState,
                workId = entry.work.id,
                workTitle = entry.work.title,
                noEpisodes = entry.work.noEpisodes
            )
        }
    }

    fun changeStatus(status: StatusState) {
        val workId = _state.value.workId
        val previous = _state.value.selectedStatus
        // Optimistically update UI
        _state.update { it.copy(isStatusChanging = true, statusChangeError = null, selectedStatus = status) }
        viewModelScope.launch {
            updateViewStateUseCase(workId, status)
                .onSuccess {
                    _events.emit(WatchingEpisodeModalEvent.StatusChanged)
                }
                .onFailure { e ->
                    val errorMessage = errorMapper.toUserMessage(e, "WatchingEpisodeModalViewModel.changeStatus")
                    // Roll back to previous status and show error
                    _state.update {
                        it.copy(
                            statusChangeError = errorMessage,
                            selectedStatus = previous
                        )
                    }
                    Timber.e(e, "ステータス変更に失敗: $errorMessage")
                }
            _state.update { it.copy(isStatusChanging = false) }
        }
    }

    fun recordEpisode() {
        val episode = _state.value.episode ?: return
        val workId = _state.value.workId
        val status = _state.value.selectedStatus ?: return

        viewModelScope.launch {
            watchEpisodeUseCase(episode.id, workId, status)
                .onSuccess {
                    _events.emit(WatchingEpisodeModalEvent.EpisodeRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "WatchingEpisodeModalViewModel.recordEpisode")
                    Timber.e(e, "WatchingEpisodeModal: エピソード記録に失敗 - $msg")
                    _state.update { it.copy(statusChangeError = msg) }
                }
        }
    }
}
