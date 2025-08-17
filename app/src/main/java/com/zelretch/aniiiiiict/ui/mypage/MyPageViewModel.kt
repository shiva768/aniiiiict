package com.zelretch.aniiiiiict.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.domain.usecase.GetMyActivitiesUseCase
import com.zelretch.aniiiiiict.domain.usecase.MyActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MyPageUiState(
    val isLoading: Boolean = false,
    // Data structure: Year -> Month -> List of activities
    val activitiesByMonth: Map<Int, Map<Int, List<MyActivity>>> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getMyActivitiesUseCase: GetMyActivitiesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadActivities()
    }

    private fun loadActivities() {
        _uiState.update { it.copy(isLoading = true) }
        getMyActivitiesUseCase()
            .onEach { activities ->
                val grouped = activities.groupBy { it.record.createdAt.year }
                    .mapValues { entry ->
                        entry.value.groupBy { it.record.createdAt.monthValue }
                    }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activitiesByMonth = grouped
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
