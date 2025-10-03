package com.zelretch.aniiiiict.ui.animedetail

import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiict.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

data class AnimeDetailUiState(
    val animeDetailInfo: AnimeDetailInfo? = null,
    override val isLoading: Boolean = true,
    override val error: String? = null
) : BaseUiState(isLoading, error)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(AnimeDetailUiState())
    val state: StateFlow<AnimeDetailUiState> = _state.asStateFlow()

    override fun updateLoadingState(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _state.update { it.copy(error = error) }
    }

    fun loadAnimeDetail(programWithWork: ProgramWithWork) {
        executeWithLoading {
            val animeDetailInfo = getAnimeDetailUseCase(programWithWork).getOrThrow()
            _state.update { it.copy(animeDetailInfo = animeDetailInfo) }
            Timber.i("アニメ詳細情報を取得しました: ${animeDetailInfo.work.title}")
        }
    }
}
