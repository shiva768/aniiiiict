package com.zelretch.aniiiiict.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.BulkRecordResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
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

data class BroadcastEpisodeModalState(
    val programs: List<Program> = emptyList(),
    val showConfirmDialog: Boolean = false,
    val selectedEpisodeIndex: Int? = null,
    val selectedStatus: StatusState? = null,
    val isStatusChanging: Boolean = false,
    val statusChangeError: String? = null,
    val workId: String = "",
    val malAnimeId: String? = null,
    val isBulkRecording: Boolean = false,
    val bulkRecordingProgress: Int = 0,
    val bulkRecordingTotal: Int = 0,
    val showFinaleConfirmation: Boolean = false,
    val finaleEpisodeNumber: Int? = null,
    val showSingleEpisodeFinaleConfirmation: Boolean = false,
    val singleEpisodeFinaleNumber: Int? = null,
    val singleEpisodeFinaleWorkId: String? = null
)

sealed interface BroadcastEpisodeModalEvent {
    object StatusChanged : BroadcastEpisodeModalEvent
    object EpisodesRecorded : BroadcastEpisodeModalEvent
    object BulkEpisodesRecorded : BroadcastEpisodeModalEvent
    object FinaleConfirmationShown : BroadcastEpisodeModalEvent
}

/**
 * BroadcastEpisodeModal画面のViewModel
 * Track画面で放送スケジュールからエピソードを記録するために使用
 *
 * Now in Android パターンへの移行:
 * - ErrorMapperによるユーザー向けメッセージ変換
 * - 明示的なResult handling
 *
 * Note: イベント駆動型の複雑なUI状態管理があるため、
 * 現時点では従来のStateパターンを維持。
 */
@HiltViewModel
class BroadcastEpisodeModalViewModel @Inject constructor(
    private val bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val judgeFinaleUseCase: JudgeFinaleUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _state = MutableStateFlow(BroadcastEpisodeModalState())
    val state: StateFlow<BroadcastEpisodeModalState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BroadcastEpisodeModalEvent>()
    val events: SharedFlow<BroadcastEpisodeModalEvent> = _events.asSharedFlow()

    fun initialize(programWithWork: ProgramWithWork) {
        _state.update {
            it.copy(
                programs = programWithWork.programs,
                selectedStatus = programWithWork.work.viewerStatusState,
                workId = programWithWork.work.id,
                malAnimeId = programWithWork.work.malAnimeId
            )
        }
    }

    fun showConfirmDialog(index: Int) {
        if (index <= 0) {
            // 単一エピソードの場合は確認ダイアログを出さず、そのまま記録を実行する
            val firstProgram = _state.value.programs.firstOrNull()
            val status = _state.value.selectedStatus
            val episodeId = firstProgram?.episode?.id
            if (episodeId != null && status != null) {
                // bulkRecordEpisodes 内で selectedEpisodeIndex を参照するため事前に設定
                _state.update { it.copy(showConfirmDialog = false, selectedEpisodeIndex = 0) }
                bulkRecordEpisodes(listOf(episodeId), status)
            } else {
                // データが不足している場合はフォールバックとしてダイアログも出さない
                _state.update { it.copy(showConfirmDialog = false, selectedEpisodeIndex = null) }
            }
        } else {
            _state.update { it.copy(showConfirmDialog = true, selectedEpisodeIndex = index) }
        }
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
            updateViewStateUseCase(workId, status)
                .onSuccess {
                    // Keep the selected status (already set) and notify
                    _events.emit(BroadcastEpisodeModalEvent.StatusChanged)
                }
                .onFailure { e ->
                    val errorMessage = errorMapper.toUserMessage(e, "BroadcastEpisodeModalViewModel.changeStatus")
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

    fun recordEpisode(episodeId: String, status: StatusState) {
        val workId = _state.value.workId
        // エピソード情報をフィナーレ判定用に事前に取得
        val currentEpisode = _state.value.programs.find { it.episode.id == episodeId }
        Timber.d(
            "DetailModal: recordEpisode - episodeId=$episodeId, workId=$workId, " +
                "currentEpisode=${currentEpisode?.episode?.number}, malAnimeId=${_state.value.malAnimeId}"
        )
        viewModelScope.launch {
            watchEpisodeUseCase(episodeId, workId, status)
                .onSuccess {
                    Timber.d(
                        "DetailModal: recordEpisode - watchEpisodeUseCase succeeded, " +
                            "calling handleSingleEpisodeFinaleJudgement"
                    )
                    // フィナーレ判定を実行（エピソードを削除する前に）
                    handleSingleEpisodeFinaleJudgement(currentEpisode, workId)

                    // 記録したエピソードのプログラムを表示から消す
                    _state.update {
                        it.copy(
                            programs = _state.value.programs.filter { it.episode.id != episodeId }
                        )
                    }
                    _events.emit(BroadcastEpisodeModalEvent.EpisodesRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "BroadcastEpisodeModalViewModel.recordEpisode")
                    Timber.e(e, "DetailModal: エピソード記録に失敗 - $msg")
                }
        }
    }

    private suspend fun handleSingleEpisodeFinaleJudgement(currentEpisode: Program?, workId: String) {
        if (currentEpisode?.episode?.number == null) {
            Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                    "currentEpisode is null or episode number is null"
            )
            return
        }

        val episodeNumber = currentEpisode.episode.number
        val malAnimeIdString = _state.value.malAnimeId
        Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                "episodeNumber=$episodeNumber, malAnimeIdString=$malAnimeIdString"
        )

        if (malAnimeIdString.isNullOrEmpty()) {
            Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - malAnimeIdString is null or empty"
            )
            return
        }

        val malAnimeId = malAnimeIdString.toIntOrNull()
        if (malAnimeId == null) {
            Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - malAnimeId could not be parsed as int"
            )
            return
        }

        val hasNextEpisode = currentEpisode.episode.hasNextEpisode
        Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - calling judgeFinaleUseCase " +
                "with episodeNumber=$episodeNumber, malAnimeId=$malAnimeId, hasNextEpisode=$hasNextEpisode"
        )
        val judgeResult = judgeFinaleUseCase(episodeNumber, malAnimeId, hasNextEpisode)
        Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                "judgeResult.isFinale=${judgeResult.isFinale}, judgeResult.state=${judgeResult.state}"
        )

        if (judgeResult.isFinale) {
            Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - finale detected, updating state"
            )
            _state.update {
                it.copy(
                    showSingleEpisodeFinaleConfirmation = true,
                    singleEpisodeFinaleNumber = episodeNumber,
                    singleEpisodeFinaleWorkId = workId
                )
            }
            _events.emit(BroadcastEpisodeModalEvent.FinaleConfirmationShown)
        } else {
            Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - not a finale"
            )
        }
    }

    fun bulkRecordEpisodes(episodeIds: List<String>, status: StatusState) {
        val currentState = _state.value
        val lastEpisode = currentState.programs
            .find { it.episode.id == episodeIds.lastOrNull() }
            ?.episode

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
                    workId = currentState.workId,
                    currentStatus = status,
                    malAnimeId = currentState.malAnimeId?.toIntOrNull(),
                    lastEpisodeNumber = lastEpisode?.number,
                    lastEpisodeHasNext = lastEpisode?.hasNextEpisode,
                    onProgress = { progress ->
                        _state.update { it.copy(bulkRecordingProgress = progress) }
                    }
                ).getOrThrow()
            }.onSuccess { result ->
                handleBulkRecordSuccess(result, lastEpisode?.number)
            }.onFailure { e ->
                handleBulkRecordFailure(e)
            }
        }
    }

    private suspend fun handleBulkRecordSuccess(result: BulkRecordResult, lastEpisodeNumber: Int?) {
        val currentPrograms = _state.value.programs
        val targetPrograms = currentPrograms.filterIndexed { index, _ ->
            index <= (_state.value.selectedEpisodeIndex ?: return)
        }

        val shouldShowFinaleConfirmation = result.finaleResult?.isFinale == true

        _state.update {
            it.copy(
                programs = currentPrograms - targetPrograms.toSet(),
                showConfirmDialog = false,
                selectedEpisodeIndex = null,
                isBulkRecording = false,
                bulkRecordingProgress = 0,
                bulkRecordingTotal = 0,
                showFinaleConfirmation = shouldShowFinaleConfirmation,
                finaleEpisodeNumber = if (shouldShowFinaleConfirmation) lastEpisodeNumber else null
            )
        }

        val event = if (shouldShowFinaleConfirmation) {
            BroadcastEpisodeModalEvent.FinaleConfirmationShown
        } else {
            BroadcastEpisodeModalEvent.BulkEpisodesRecorded
        }
        _events.emit(event)
    }

    private fun handleBulkRecordFailure(e: Throwable) {
        val msg = errorMapper.toUserMessage(e, "BroadcastEpisodeModalViewModel.bulkRecordEpisodes")
        Timber.e(e, "DetailModal: 一括記録に失敗 - $msg")
        _state.update {
            it.copy(
                isBulkRecording = false,
                bulkRecordingProgress = 0,
                bulkRecordingTotal = 0
            )
        }
    }

    fun confirmFinaleWatched() {
        val workId = _state.value.workId
        viewModelScope.launch {
            updateViewStateUseCase(workId, StatusState.WATCHED)
                .onSuccess {
                    _state.update {
                        it.copy(
                            showFinaleConfirmation = false,
                            finaleEpisodeNumber = null
                        )
                    }
                    _events.emit(BroadcastEpisodeModalEvent.BulkEpisodesRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "BroadcastEpisodeModalViewModel.confirmFinaleWatched")
                    Timber.e(e, "DetailModal: フィナーレ確認に失敗 - $msg")
                }
        }
    }

    fun hideFinaleConfirmation() {
        _state.update {
            it.copy(
                showFinaleConfirmation = false,
                finaleEpisodeNumber = null
            )
        }
        viewModelScope.launch {
            _events.emit(BroadcastEpisodeModalEvent.BulkEpisodesRecorded)
        }
    }

    fun confirmSingleEpisodeFinaleWatched() {
        val workId = _state.value.singleEpisodeFinaleWorkId ?: return
        viewModelScope.launch {
            updateViewStateUseCase(workId, StatusState.WATCHED)
                .onSuccess {
                    _state.update {
                        it.copy(
                            showSingleEpisodeFinaleConfirmation = false,
                            singleEpisodeFinaleNumber = null,
                            singleEpisodeFinaleWorkId = null
                        )
                    }
                    _events.emit(BroadcastEpisodeModalEvent.EpisodesRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(
                        e,
                        "BroadcastEpisodeModalViewModel.confirmSingleEpisodeFinaleWatched"
                    )
                    Timber.e(e, "DetailModal: 単一エピソードフィナーレ確認に失敗 - $msg")
                }
        }
    }

    fun hideSingleEpisodeFinaleConfirmation() {
        _state.update {
            it.copy(
                showSingleEpisodeFinaleConfirmation = false,
                singleEpisodeFinaleNumber = null,
                singleEpisodeFinaleWorkId = null
            )
        }
        viewModelScope.launch {
            _events.emit(BroadcastEpisodeModalEvent.EpisodesRecorded)
        }
    }
}
