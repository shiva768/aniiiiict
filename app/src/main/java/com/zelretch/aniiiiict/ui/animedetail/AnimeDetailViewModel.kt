package com.zelretch.aniiiiict.ui.animedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
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
    val selectedStatus: StatusState? = null,
    val isStatusChanging: Boolean = false,
    val statusChangeError: String? = null
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
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AnimeDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AnimeDetailData>> = _uiState.asStateFlow()

    private var workId: String = ""

    /**
     * アニメ詳細情報を読み込む
     *
     * @param programWithWork 番組情報
     */
    fun loadAnimeDetail(programWithWork: ProgramWithWork) {
        workId = programWithWork.work.id
        launchWithMinLoadingTime {
            // ローディング状態に設定
            _uiState.value = UiState.Loading

            // データ取得
            getAnimeDetailUseCase(programWithWork)
                .onSuccess { animeDetailInfo ->
                    _uiState.value = UiState.Success(
                        AnimeDetailData(
                            animeDetailInfo = animeDetailInfo,
                            selectedStatus = programWithWork.work.viewerStatusState
                        )
                    )
                    Timber.i("アニメ詳細情報を取得しました: ${animeDetailInfo.work.title}")
                }
                .onFailure { error ->
                    val message = errorMapper.toUserMessage(error, "AnimeDetailViewModel.loadAnimeDetail")
                    _uiState.value = UiState.Error(message)
                    Timber.e(error, "アニメ詳細情報の取得に失敗: $message")
                }
        }
    }

    /**
     * 作品のステータスを変更する
     *
     * @param status 変更後のステータス
     */
    fun changeStatus(status: StatusState) {
        val currentData = (_uiState.value as? UiState.Success)?.data ?: return
        val previous = currentData.selectedStatus
        _uiState.value = UiState.Success(
            currentData.copy(
                isStatusChanging = true,
                statusChangeError = null,
                selectedStatus = status
            )
        )
        viewModelScope.launch {
            updateViewStateUseCase(workId, status)
                .onSuccess {
                    Timber.i("ステータスを変更しました: $status")
                }
                .onFailure { e ->
                    val message = errorMapper.toUserMessage(e, "AnimeDetailViewModel.changeStatus")
                    (_uiState.value as? UiState.Success)?.data?.let { data ->
                        _uiState.value = UiState.Success(
                            data.copy(
                                selectedStatus = previous,
                                isStatusChanging = false,
                                statusChangeError = message
                            )
                        )
                    }
                    Timber.e(e, "ステータス変更に失敗: $message")
                    return@launch
                }
            (_uiState.value as? UiState.Success)?.data?.let { data ->
                _uiState.value = UiState.Success(data.copy(isStatusChanging = false))
            }
        }
    }
}
