package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val records: List<com.zelretch.aniiiiiict.data.model.Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val isRecording: Boolean = false,
    val recordingSuccess: String? = null,
    val filterState: FilterState = FilterState(),
    val isFilterVisible: Boolean = false,
    val availableMedia: List<String> = emptyList(),
    val availableSeasons: List<String> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableChannels: List<String> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AnnictRepository,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val programFilter: ProgramFilter
) : BaseViewModel() {
    private val TAG = "MainViewModel"
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var isAuthInProgress = false

    init {
        AniiiiiictLogger.logInfo(TAG, "koko", "koko")
        checkAuthState { loadInitialData() }
    }

    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    private fun checkAuthState(function: () -> Unit) {
        viewModelScope.launch {
            try {
                val isAuthenticated = repository.isAuthenticated()

                // 認証されていない場合は認証を開始
                if (!isAuthenticated) {
                    AniiiiiictLogger.logInfo(
                        TAG,
                        "認証されていないため、認証を開始します",
                        "checkAuthState"
                    )
                    startAuth()
                } else {
                    function()
                }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(TAG, e, "認証状態の確認中にエラーが発生")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "認証状態の確認に失敗しました",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        executeWithLoading {
            loadPrograms()
            loadRecords()
        }
    }

    suspend fun loadPrograms() {
        repository.getProgramsWithWorks()
            .collect { programs ->
                _uiState.update { currentState ->
                    val filteredPrograms =
                        programFilter.applyFilters(programs, currentState.filterState)
                    val availableFilters = programFilter.extractAvailableFilters(programs)
                    currentState.copy(
                        programs = filteredPrograms,
                        isAuthenticating = false,
                        availableMedia = availableFilters.media,
                        availableSeasons = availableFilters.seasons,
                        availableYears = availableFilters.years,
                        availableChannels = availableFilters.channels
                    )
                }
                preloadImages(_uiState.value.programs)
            }
    }

    private suspend fun loadRecords() {
        val result = repository.getRecords(null)
        _uiState.update { it.copy(records = result.records) }
    }

    private fun preloadImages(programs: List<ProgramWithWork>) {
        viewModelScope.launch(Dispatchers.IO) {
            programs.forEach { program ->
                if (!isActive) return@forEach

                try {
                    val imageUrl = program.work.image?.recommendedImageUrl.takeIf {
                        !it.isNullOrEmpty() && it.startsWith("http", ignoreCase = true)
                    }

                    if (imageUrl == null) {
                        AniiiiiictLogger.logInfo(
                            TAG,
                            "有効な画像URLがないためスキップ: ${program.work.title}",
                            "MainViewModel.preloadImages"
                        )
                        return@forEach
                    }

                    val workId = program.work.annictId
                    val success = repository.saveWorkImage(workId, imageUrl)
                    if (success) {
                        AniiiiiictLogger.logInfo(
                            TAG,
                            "画像を保存しました: ${program.work.title}",
                            "MainViewModel.preloadImages"
                        )
                    } else {
                        AniiiiiictLogger.logWarning(
                            TAG,
                            "画像の保存に失敗: ${program.work.title}",
                            "MainViewModel.preloadImages"
                        )
                    }
                } catch (e: Exception) {
                    AniiiiiictLogger.logError(
                        TAG,
                        e,
                        "画像のプリロードに失敗: ${program.work.title}"
                    )
                }
            }
        }
    }

    fun onImageLoad(annictId: Int, imageUrl: String) {
        viewModelScope.launch {
            try {
                if (imageUrl.isBlank() || !imageUrl.startsWith("http", ignoreCase = true)) {
                    AniiiiiictLogger.logWarning(TAG, "無効な画像URL: '$imageUrl'", "onImageLoad")
                    return@launch
                }

                repository.saveWorkImage(annictId.toLong(), imageUrl)
            } catch (e: Exception) {
                AniiiiiictLogger.logError(TAG, e, "画像の保存に失敗: $imageUrl")
            }
        }
    }

    fun startAuth() {
        if (isAuthInProgress) {
            AniiiiiictLogger.logWarning(TAG, "認証が既に進行中です", "startAuth")
            return
        }
        AniiiiiictLogger.logInfo(TAG, "authsiteru", "auth")
        isAuthInProgress = true
        _uiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                val authUrl = repository.getAuthUrl()
                AniiiiiictLogger.logInfo(TAG, "認証URLを取得: $authUrl", "startAuth")

                delay(200)

                if (!isActive) return@launch

                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                AniiiiiictLogger.logError(TAG, e, "認証URLの取得に失敗")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "認証に失敗しました",
                        isLoading = false,
                        isAuthenticating = false
                    )
                }
                isAuthInProgress = false
            }
        }
    }

    fun handleAuthCallback(code: String?) {
        if (isAuthInProgress) {
            viewModelScope.launch {
                try {
                    if (code != null) {
                        println("MainViewModel: 認証コードを処理中: ${code.take(5)}...")
                        delay(200)

                        if (!isActive) return@launch

                        val success = repository.handleAuthCallback(code)
                        if (success) {
                            println("MainViewModel: 認証成功")
                            delay(300)
                            _uiState.update { it.copy(isAuthenticating = false) }
                            loadInitialData()
                        } else {
                            AniiiiiictLogger.logWarning(
                                TAG,
                                "認証が失敗しました",
                                "handleAuthCallback"
                            )
                            println("MainViewModel: 認証失敗")
                            delay(200)
                            _uiState.update {
                                it.copy(
                                    error = "認証に失敗しました。再度お試しください。",
                                    isLoading = false,
                                    isAuthenticating = false
                                )
                            }
                            isAuthInProgress = false
                        }
                    } else {
                        AniiiiiictLogger.logWarning(
                            TAG,
                            "認証コードがnullです",
                            "handleAuthCallback"
                        )
                        println("MainViewModel: 認証コードなし")
                        delay(200)
                        _uiState.update {
                            it.copy(
                                error = "認証に失敗しました。再度お試しください。",
                                isLoading = false,
                                isAuthenticating = false
                            )
                        }
                        isAuthInProgress = false
                    }
                } catch (e: Exception) {
                    AniiiiiictLogger.logError(TAG, e, "認証処理に失敗")
                    e.printStackTrace()
                    delay(200)
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "認証に失敗しました",
                            isLoading = false,
                            isAuthenticating = false
                        )
                    }
                    isAuthInProgress = false
                }
            }
        }
    }

    fun cancelAuth() {
        viewModelScope.launch {
            println("MainViewModel: 認証をキャンセルします")
            isAuthInProgress = false
            _uiState.update {
                it.copy(
                    isAuthenticating = false,
                    isLoading = false
                )
            }
        }
    }

    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }

                watchEpisodeUseCase(episodeId, workId, currentStatus)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                recordingSuccess = episodeId,
                                error = null
                            )
                        }

                        delay(2000)
                        if (_uiState.value.recordingSuccess == episodeId) {
                            _uiState.update { it.copy(recordingSuccess = null) }
                        }

                        loadPrograms()
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                error = e.message ?: "エピソードの記録に失敗しました"
                            )
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
                _uiState.update { it.copy(isRecording = false) }
            }
        }
    }

    fun refresh() {
        AniiiiiictLogger.logInfo(TAG, "プログラム一覧を再読み込み", "MainViewModel.refresh")
        checkAuthState { loadInitialData() }
    }

    fun updateFilter(
        selectedMedia: String? = _uiState.value.filterState.selectedMedia,
        selectedSeason: String? = _uiState.value.filterState.selectedSeason,
        selectedYear: Int? = _uiState.value.filterState.selectedYear,
        selectedChannel: String? = _uiState.value.filterState.selectedChannel,
        selectedStatus: StatusState? = _uiState.value.filterState.selectedStatus,
        searchQuery: String = _uiState.value.filterState.searchQuery
    ) {
        _uiState.update { currentState ->
            val newFilterState = currentState.filterState.copy(
                selectedMedia = selectedMedia,
                selectedSeason = selectedSeason,
                selectedYear = selectedYear,
                selectedChannel = selectedChannel,
                selectedStatus = selectedStatus,
                searchQuery = searchQuery
            )
            currentState.copy(
                filterState = newFilterState,
                programs = programFilter.applyFilters(currentState.programs, newFilterState)
            )
        }
        viewModelScope.launch {
            loadPrograms()
        }
    }

    fun toggleFilterVisibility() {
        _uiState.update {
            it.copy(
                isFilterVisible = !it.isFilterVisible
            )
        }
    }

    private fun handleError(error: Throwable) {
        AniiiiiictLogger.logError(TAG, error, "MainViewModel")
        _uiState.update { it.copy(error = error.message) }
    }
}