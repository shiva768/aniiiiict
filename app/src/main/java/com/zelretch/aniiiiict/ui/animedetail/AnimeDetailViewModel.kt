package com.zelretch.aniiiiict.ui.animedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import com.zelretch.aniiiiict.ui.base.UiState
import com.zelretch.aniiiiict.ui.base.launchWithMinLoadingTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * アニメ詳細画面のデータクラス
 */
data class AnimeDetailData(
    val animeDetailInfo: AnimeDetailInfo,
    val isRecording: Boolean = false,
    val recordingSuccess: Boolean = false
)

/**
 * アニメ詳細画面のViewModel
 *
 * Now in Android パターンに準拠:
 * - BaseViewModelを使用せず、明示的なエラーハンドリング
 * - UiState<T> sealed interfaceによる統一された状態管理
 * - ErrorMapperによるユーザー向けメッセージ変換
 */
@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AnimeDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AnimeDetailData>> = _uiState.asStateFlow()

    /**
     * アニメ詳細情報を読み込む
     *
     * @param programWithWork 番組情報
     */
    fun loadAnimeDetail(programWithWork: ProgramWithWork) {
        launchWithMinLoadingTime {
            // ローディング状態に設定
            _uiState.value = UiState.Loading

            // データ取得
            getAnimeDetailUseCase(programWithWork)
                .onSuccess { animeDetailInfo ->
                    _uiState.value = UiState.Success(AnimeDetailData(animeDetailInfo))
                    Timber.i("アニメ詳細情報を取得しました: ${animeDetailInfo.work.title}")
                }
                .onFailure { error ->
                    val message = errorMapper.toUserMessage(error, "AnimeDetailViewModel.loadAnimeDetail")
                    _uiState.value = UiState.Error(message)
                    Timber.e(error, "アニメ詳細情報の取得に失敗: $message")
                }
        }
    }

    fun recordNextEpisode(programWithWork: ProgramWithWork) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                _uiState.value = currentState.copy(data = currentState.data.copy(isRecording = true))
                val nextEpisode = programWithWork.firstProgram.episode
                watchEpisodeUseCase(
                    episodeId = nextEpisode.id,
                    workId = programWithWork.work.id,
                    currentStatus = programWithWork.work.viewerStatusState
                ).onSuccess {
                    _uiState.value =
                        currentState.copy(data = currentState.data.copy(isRecording = false, recordingSuccess = true))
                    loadAnimeDetail(programWithWork)
                }.onFailure { error ->
                    val message = errorMapper.toUserMessage(error, "AnimeDetailViewModel.recordNextEpisode")
                    _uiState.value = UiState.Error(message)
                }
            }
        }
    }

    fun updateStatus(programWithWork: ProgramWithWork, status: StatusState) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                updateViewStateUseCase(programWithWork.work.id, status)
                    .onSuccess {
                        loadAnimeDetail(programWithWork)
                    }.onFailure { error ->
                        val message = errorMapper.toUserMessage(error, "AnimeDetailViewModel.updateStatus")
                        _uiState.value = UiState.Error(message)
                    }
            }
        }
    }
}
