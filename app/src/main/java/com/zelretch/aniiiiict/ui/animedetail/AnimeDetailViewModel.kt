package com.zelretch.aniiiiict.ui.animedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AnimeDetailState(
    val isLoading: Boolean = true,
    val animeDetailInfo: AnimeDetailInfo? = null,
    val error: String? = null
)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AnimeDetailState())
    val state: StateFlow<AnimeDetailState> = _state.asStateFlow()

    fun loadAnimeDetail(programWithWork: ProgramWithWork) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getAnimeDetailUseCase(programWithWork)
                .onSuccess { animeDetailInfo ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            animeDetailInfo = animeDetailInfo,
                            error = null
                        )
                    }
                    Timber.i("アニメ詳細情報を取得しました: ${animeDetailInfo.work.title}")
                }
                .onFailure { error ->
                    val errorMessage = "アニメ詳細情報の取得に失敗しました: ${error.message}"
                    Timber.e(error, errorMessage)
                    ErrorHandler.handleError(error, "AnimeDetailViewModel", "loadAnimeDetail")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
        }
    }
}
