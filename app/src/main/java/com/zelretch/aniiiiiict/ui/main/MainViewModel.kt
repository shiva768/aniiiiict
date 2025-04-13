package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.domain.filter.SortOrder
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
    val availableChannels: List<String> = emptyList(),
    val allPrograms: List<ProgramWithWork> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AnnictRepository,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val programFilter: ProgramFilter,
    private val filterPreferences: FilterPreferences,
    @ApplicationContext private val context: Context
) : BaseViewModel() {
    private val TAG = "MainViewModel"

    // UI状態のカプセル化
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // フィルター状態の復元を待ってから認証状態のチェックを行う
            filterPreferences.filterState.collect { savedFilterState ->
                if (_uiState.value.allPrograms.isEmpty()) {
                    // 初回のみ認証チェックを行う
                    _uiState.update { currentState ->
                        currentState.copy(filterState = savedFilterState)
                    }
                    checkAuthState()
                } else {
                    // 2回目以降はフィルター状態の更新のみ
                    _uiState.update { currentState ->
                        currentState.copy(
                            filterState = savedFilterState,
                            programs = programFilter.applyFilters(
                                currentState.allPrograms,
                                savedFilterState
                            )
                        )
                    }
                }
            }
        }
    }

    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    // 認証状態の確認（内部メソッド）
    private fun checkAuthState() {
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
                    loadingPrograms()
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

    // 番組一覧の読み込み（内部メソッド）
    private fun loadingPrograms() {
        executeWithLoading {
            loadPrograms()
        }
    }

    // 番組一覧の読み込み（内部メソッド）
    private suspend fun loadPrograms() {
        repository.getProgramsWithWorks()
            .collect { programs ->
                _uiState.update { currentState ->
                    val availableFilters = programFilter.extractAvailableFilters(programs)
                    val filteredPrograms =
                        programFilter.applyFilters(programs, currentState.filterState)
                    currentState.copy(
                        programs = filteredPrograms,
                        isAuthenticating = false,
                        availableMedia = availableFilters.media,
                        availableSeasons = availableFilters.seasons,
                        availableYears = availableFilters.years,
                        availableChannels = availableFilters.channels,
                        allPrograms = programs
                    )
                }
                preloadImages(_uiState.value.programs)
            }
    }

    // 画像のプリロード（内部メソッド）
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

    // 画像の読み込み（公開メソッド）
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

    // 認証開始（公開メソッド）
    fun startAuth() {
        AniiiiiictLogger.logInfo(TAG, "authsiteru", "auth")
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
            }
        }
    }

    // 認証コールバック処理（公開メソッド）
    fun handleAuthCallback(code: String?) {
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
                        loadingPrograms()
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
            }
        }
    }

    // エピソードの記録（公開メソッド）
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

                        loadingPrograms()
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

    // 再読み込み（公開メソッド）
    fun refresh() {
        AniiiiiictLogger.logInfo(TAG, "プログラム一覧を再読み込み", "MainViewModel.refresh")
        checkAuthState()
    }

    // フィルターの更新（公開メソッド）
    fun updateFilter(
        selectedMedia: Set<String> = _uiState.value.filterState.selectedMedia,
        selectedSeason: Set<String> = _uiState.value.filterState.selectedSeason,
        selectedYear: Set<Int> = _uiState.value.filterState.selectedYear,
        selectedChannel: Set<String> = _uiState.value.filterState.selectedChannel,
        selectedStatus: Set<StatusState> = _uiState.value.filterState.selectedStatus,
        searchQuery: String = _uiState.value.filterState.searchQuery,
        showOnlyAired: Boolean = _uiState.value.filterState.showOnlyAired,
        sortOrder: SortOrder = _uiState.value.filterState.sortOrder
    ) {
        _uiState.update { currentState ->
            val newFilterState = currentState.filterState.copy(
                selectedMedia = selectedMedia,
                selectedSeason = selectedSeason,
                selectedYear = selectedYear,
                selectedChannel = selectedChannel,
                selectedStatus = selectedStatus,
                searchQuery = searchQuery,
                showOnlyAired = showOnlyAired,
                sortOrder = sortOrder
            )
            currentState.copy(
                filterState = newFilterState,
                programs = programFilter.applyFilters(currentState.allPrograms, newFilterState)
            )
        }
        // フィルター状態の保存
        viewModelScope.launch {
            filterPreferences.updateFilterState(_uiState.value.filterState)
        }
    }

    // フィルターの表示/非表示の切り替え（公開メソッド）
    fun toggleFilterVisibility() {
        _uiState.update {
            it.copy(
                isFilterVisible = !it.isFilterVisible
            )
        }
    }

    // エラー処理（内部メソッド）
    private fun handleError(error: Throwable) {
        AniiiiiictLogger.logError(TAG, error, "MainViewModel")
        _uiState.update { it.copy(error = error.message) }
    }
}