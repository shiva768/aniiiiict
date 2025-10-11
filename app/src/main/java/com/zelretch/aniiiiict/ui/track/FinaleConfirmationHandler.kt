package com.zelretch.aniiiiict.ui.track

import com.annict.type.StatusState
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * フィナーレ確認処理を担当するヘルパークラス
 *
 * BroadcastEpisodeModalViewModelから抽出し、フィナーレ確認関連の処理をまとめる。
 */
class FinaleConfirmationHandler @Inject constructor(
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val errorMapper: ErrorMapper
) {
    /**
     * バルク記録のフィナーレ確認
     */
    fun confirmBulkFinaleWatched(
        state: MutableStateFlow<BroadcastEpisodeModalState>,
        events: MutableSharedFlow<BroadcastEpisodeModalEvent>,
        scope: CoroutineScope
    ) {
        val workId = state.value.workId
        scope.launch {
            updateViewStateUseCase(workId, StatusState.WATCHED)
                .onSuccess {
                    state.update {
                        it.copy(
                            showFinaleConfirmation = false,
                            finaleEpisodeNumber = null
                        )
                    }
                    events.emit(BroadcastEpisodeModalEvent.BulkEpisodesRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "FinaleConfirmationHandler.confirmBulkFinaleWatched")
                    Timber.e(e, "DetailModal: フィナーレ確認に失敗 - $msg")
                }
        }
    }

    /**
     * バルク記録のフィナーレ確認を非表示
     */
    fun hideBulkFinaleConfirmation(
        state: MutableStateFlow<BroadcastEpisodeModalState>,
        events: MutableSharedFlow<BroadcastEpisodeModalEvent>,
        scope: CoroutineScope
    ) {
        state.update {
            it.copy(
                showFinaleConfirmation = false,
                finaleEpisodeNumber = null
            )
        }
        scope.launch {
            events.emit(BroadcastEpisodeModalEvent.BulkEpisodesRecorded)
        }
    }

    /**
     * 単一エピソードのフィナーレ確認
     */
    fun confirmSingleFinaleWatched(
        state: MutableStateFlow<BroadcastEpisodeModalState>,
        events: MutableSharedFlow<BroadcastEpisodeModalEvent>,
        scope: CoroutineScope
    ) {
        val workId = state.value.singleEpisodeFinaleWorkId ?: return
        scope.launch {
            updateViewStateUseCase(workId, StatusState.WATCHED)
                .onSuccess {
                    state.update {
                        it.copy(
                            showSingleEpisodeFinaleConfirmation = false,
                            singleEpisodeFinaleNumber = null,
                            singleEpisodeFinaleWorkId = null
                        )
                    }
                    events.emit(BroadcastEpisodeModalEvent.EpisodesRecorded)
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(
                        e,
                        "FinaleConfirmationHandler.confirmSingleFinaleWatched"
                    )
                    Timber.e(e, "DetailModal: 単一エピソードフィナーレ確認に失敗 - $msg")
                }
        }
    }

    /**
     * 単一エピソードのフィナーレ確認を非表示
     */
    fun hideSingleFinaleConfirmation(
        state: MutableStateFlow<BroadcastEpisodeModalState>,
        events: MutableSharedFlow<BroadcastEpisodeModalEvent>,
        scope: CoroutineScope
    ) {
        state.update {
            it.copy(
                showSingleEpisodeFinaleConfirmation = false,
                singleEpisodeFinaleNumber = null,
                singleEpisodeFinaleWorkId = null
            )
        }
        scope.launch {
            events.emit(BroadcastEpisodeModalEvent.EpisodesRecorded)
        }
    }
}
