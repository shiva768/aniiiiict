package com.zelretch.aniiiiiict.ui.track

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
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiState(
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
    val availableSeasons: List<SeasonName> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableChannels: List<String> = emptyList(),
    val allPrograms: List<ProgramWithWork> = emptyList(),
    val selectedProgram: ProgramWithWork? = null,
    val isDetailModalVisible: Boolean = false,
    val isLoadingDetail: Boolean = false
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val repository: AnnictRepository,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val programFilter: ProgramFilter,
    private val filterPreferences: FilterPreferences,
    logger: Logger,
    @ApplicationContext private val context: Context
) : BaseViewModel(logger) {
    private val TAG = "TrackViewModel"

    // UI状態のカプセル化
    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

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
                    logger.logInfo(
                        TAG,
                        "認証されていないため、認証を開始します",
                        "checkAuthState"
                    )
                    startAuth()
                } else {
                    loadingPrograms()
                }
            } catch (e: Exception) {
                logger.logError(TAG, e, "認証状態の確認中にエラーが発生")
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
            }
    }

    // 認証開始（公開メソッド）
    fun startAuth() {
        _uiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                val authUrl = repository.getAuthUrl()
                logger.logInfo(TAG, "認証URLを取得: $authUrl", "startAuth")

                delay(200)

                if (!isActive) return@launch

                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                logger.logError(TAG, e, "認証URLの取得に失敗")
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
                    println("TrackViewModel: 認証コードを処理中: ${code.take(5)}...")
                    delay(200)

                    if (!isActive) return@launch

                    val success = repository.handleAuthCallback(code)
                    if (success) {
                        println("TrackViewModel: 認証成功")
                        delay(300)
                        _uiState.update { it.copy(isAuthenticating = false) }
                        loadingPrograms()
                    } else {
                        logger.logWarning(
                            TAG,
                            "認証が失敗しました",
                            "handleAuthCallback"
                        )
                        println("TrackViewModel: 認証失敗")
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
                    logger.logWarning(
                        TAG,
                        "認証コードがnullです",
                        "handleAuthCallback"
                    )
                    println("TrackViewModel: 認証コードなし")
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
                handleError(e)
            }
        }
    }

    // エピソードの記録（公開メソッド）
    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }
                runCatching {
                    watchEpisodeUseCase(episodeId, workId, currentStatus).getOrThrow()
                }.onSuccess {
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
                }.onFailure { e ->
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

    // エピソードの一括記録（公開メソッド）
    fun bulkRecordEpisode(episodeIds: List<String>, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }
                runCatching {
                    episodeIds.forEach { id ->
                        watchEpisodeUseCase(id, workId, currentStatus).getOrThrow()
                    }
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            recordingSuccess = episodeIds.lastOrNull(),
                            error = null
                        )
                    }

                    delay(2000)
                    if (_uiState.value.recordingSuccess == episodeIds.lastOrNull()) {
                        _uiState.update { it.copy(recordingSuccess = null) }
                    }

                    loadingPrograms()
                }.onFailure { e ->
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
        logger.logInfo(TAG, "プログラム一覧を再読み込み", "TrackViewModel.refresh")
        checkAuthState()
    }

    // フィルターの更新（公開メソッド）
    fun updateFilter(
        selectedMedia: Set<String> = _uiState.value.filterState.selectedMedia,
        selectedSeason: Set<SeasonName> = _uiState.value.filterState.selectedSeason,
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
        logger.logError(TAG, error, "TrackViewModel")
        _uiState.update { it.copy(error = error.message) }
    }

    // 未視聴エピソードモーダルを表示
    fun showUnwatchedEpisodes(program: ProgramWithWork) {
        _uiState.update {
            it.copy(
                selectedProgram = program,
                isDetailModalVisible = true,
                isLoadingDetail = false
            )
        }
    }

    // 未視聴エピソードモーダルを非表示
    fun hideDetail() {
        _uiState.update {
            it.copy(
                isDetailModalVisible = false,
                selectedProgram = null,
                isLoadingDetail = false
            )
        }
    }
} 