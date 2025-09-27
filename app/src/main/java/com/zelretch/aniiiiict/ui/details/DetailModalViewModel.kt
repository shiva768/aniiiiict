package com.zelretch.aniiiiict.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.repository.AnimeDetailRepository
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
    val malAnimeId: String? = null,
    val isBulkRecording: Boolean = false,
    val bulkRecordingProgress: Int = 0,
    val bulkRecordingTotal: Int = 0,
    val showFinaleConfirmation: Boolean = false,
    val finaleEpisodeNumber: Int? = null,
    val showSingleEpisodeFinaleConfirmation: Boolean = false,
    val singleEpisodeFinaleNumber: Int? = null,
    val singleEpisodeFinaleWorkId: String? = null,
    val animeDetailInfo: AnimeDetailInfo? = null,
    val isLoadingDetailInfo: Boolean = false,
    val detailInfoError: String? = null
)

sealed interface DetailModalEvent {
    object StatusChanged : DetailModalEvent
    object EpisodesRecorded : DetailModalEvent
    object BulkEpisodesRecorded : DetailModalEvent
    object FinaleConfirmationShown : DetailModalEvent
}

@HiltViewModel
class DetailModalViewModel @Inject constructor(
    private val bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val judgeFinaleUseCase: JudgeFinaleUseCase,
    private val animeDetailRepository: AnimeDetailRepository
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
                malAnimeId = programWithWork.work.malAnimeId
            )
        }

        // Fetch detailed anime information
        fetchAnimeDetailInfo(programWithWork.work.id, programWithWork.work.malAnimeId)
    }

    private fun fetchAnimeDetailInfo(workId: String, malAnimeId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetailInfo = true, detailInfoError = null) }

            animeDetailRepository.getAnimeDetailInfo(workId, malAnimeId)
                .onSuccess { detailInfo ->
                    _state.update {
                        it.copy(
                            animeDetailInfo = detailInfo,
                            isLoadingDetailInfo = false
                        )
                    }
                }
                .onFailure { error ->
                    timber.log.Timber.e(error, "Failed to fetch anime detail info")
                    _state.update {
                        it.copy(
                            isLoadingDetailInfo = false,
                            detailInfoError = error.message ?: "詳細情報の取得に失敗しました"
                        )
                    }
                }
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
        // エピソード情報をフィナーレ判定用に事前に取得
        val currentEpisode = _state.value.programs.find { it.episode.id == episodeId }
        timber.log.Timber.d(
            "DetailModal: recordEpisode - episodeId=$episodeId, workId=$workId, " +
                "currentEpisode=${currentEpisode?.episode?.number}, malAnimeId=${_state.value.malAnimeId}"
        )
        viewModelScope.launch {
            runCatching {
                watchEpisodeUseCase(episodeId, workId, status).getOrThrow()
            }.onSuccess {
                timber.log.Timber.d(
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
                _events.emit(DetailModalEvent.EpisodesRecorded)
            }.onFailure { e ->
                timber.log.Timber.e(e, "DetailModal: recordEpisode - watchEpisodeUseCase failed")
                // 表示は親で処理する設計のため、ここではユーザ向け文言を整形してログ化のみ
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.recordEpisode")
                ErrorHandler.logError("DetailModalViewModel", "recordEpisode", info)
            }
        }
    }

    private suspend fun handleSingleEpisodeFinaleJudgement(currentEpisode: Program?, workId: String) {
        if (currentEpisode?.episode?.number == null) {
            timber.log.Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                    "currentEpisode is null or episode number is null"
            )
            return
        }

        val episodeNumber = currentEpisode.episode.number
        val malAnimeIdString = _state.value.malAnimeId
        timber.log.Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                "episodeNumber=$episodeNumber, malAnimeIdString=$malAnimeIdString"
        )

        if (malAnimeIdString.isNullOrEmpty()) {
            timber.log.Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - malAnimeIdString is null or empty"
            )
            return
        }

        val malAnimeId = malAnimeIdString.toIntOrNull()
        if (malAnimeId == null) {
            timber.log.Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - malAnimeId could not be parsed as int"
            )
            return
        }

        timber.log.Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - calling judgeFinaleUseCase " +
                "with episodeNumber=$episodeNumber, malAnimeId=$malAnimeId"
        )
        val judgeResult = judgeFinaleUseCase(episodeNumber, malAnimeId)
        timber.log.Timber.d(
            "DetailModal: handleSingleEpisodeFinaleJudgement - " +
                "judgeResult.isFinale=${judgeResult.isFinale}, judgeResult.state=${judgeResult.state}"
        )

        if (judgeResult.isFinale) {
            timber.log.Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - finale detected, updating state"
            )
            _state.update {
                it.copy(
                    showSingleEpisodeFinaleConfirmation = true,
                    singleEpisodeFinaleNumber = episodeNumber,
                    singleEpisodeFinaleWorkId = workId
                )
            }
            _events.emit(DetailModalEvent.FinaleConfirmationShown)
        } else {
            timber.log.Timber.d(
                "DetailModal: handleSingleEpisodeFinaleJudgement - not a finale"
            )
        }
    }

    fun bulkRecordEpisodes(episodeIds: List<String>, status: StatusState) {
        val workId = _state.value.workId
        val currentState = _state.value
        val malAnimeId = currentState.malAnimeId?.toIntOrNull()
        val lastEpisodeNumber = currentState.programs
            .find { it.episode.id == episodeIds.lastOrNull() }
            ?.episode?.number

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
                    malAnimeId = malAnimeId,
                    lastEpisodeNumber = lastEpisodeNumber,
                    onProgress = { progress ->
                        _state.update { it.copy(bulkRecordingProgress = progress) }
                    }
                ).getOrThrow()
            }.onSuccess { result ->
                val currentPrograms = _state.value.programs
                val targetPrograms = currentPrograms.filterIndexed { index, _ ->
                    index <= (_state.value.selectedEpisodeIndex ?: return@launch)
                }

                // フィナーレ判定の結果をチェック
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

                if (shouldShowFinaleConfirmation) {
                    _events.emit(DetailModalEvent.FinaleConfirmationShown)
                } else {
                    _events.emit(DetailModalEvent.BulkEpisodesRecorded)
                }
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

    fun confirmFinaleWatched() {
        val workId = _state.value.workId
        viewModelScope.launch {
            runCatching {
                updateViewStateUseCase(workId, StatusState.WATCHED)
            }.onSuccess {
                _state.update {
                    it.copy(
                        showFinaleConfirmation = false,
                        finaleEpisodeNumber = null
                    )
                }
                _events.emit(DetailModalEvent.BulkEpisodesRecorded)
            }.onFailure { e ->
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.confirmFinaleWatched")
                ErrorHandler.logError("DetailModalViewModel", "confirmFinaleWatched", info)
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
            _events.emit(DetailModalEvent.BulkEpisodesRecorded)
        }
    }

    fun confirmSingleEpisodeFinaleWatched() {
        val workId = _state.value.singleEpisodeFinaleWorkId ?: return
        viewModelScope.launch {
            runCatching {
                updateViewStateUseCase(workId, StatusState.WATCHED)
            }.onSuccess {
                _state.update {
                    it.copy(
                        showSingleEpisodeFinaleConfirmation = false,
                        singleEpisodeFinaleNumber = null,
                        singleEpisodeFinaleWorkId = null
                    )
                }
                _events.emit(DetailModalEvent.EpisodesRecorded)
            }.onFailure { e ->
                val info = ErrorHandler.analyzeError(e, "DetailModalViewModel.confirmSingleEpisodeFinaleWatched")
                ErrorHandler.logError("DetailModalViewModel", "confirmSingleEpisodeFinaleWatched", info)
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
            _events.emit(DetailModalEvent.EpisodesRecorded)
        }
    }
}
