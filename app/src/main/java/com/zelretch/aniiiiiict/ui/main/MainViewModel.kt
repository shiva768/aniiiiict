package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class FilterState(
    val selectedMedia: String? = null,
    val selectedSeason: String? = null,
    val selectedYear: Int? = null,
    val selectedChannel: String? = null,
    val selectedStatus: StatusState? = null,
    val searchQuery: String = ""
)

data class MainUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val records: List<Record> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val isRecording: Boolean = false,
    val recordingSuccess: String? = null,
    val filterState: FilterState = FilterState(),
    val availableMedia: List<String> = emptyList(),
    val availableSeasons: List<String> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableChannels: List<String> = emptyList(),
    val isFilterVisible: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AnnictRepository,
    private val watchEpisodeUseCase: WatchEpisodeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var isAuthInProgress = false

    init {
        checkAuthState()
        loadInitialData()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            AniiiiiictLogger.logInfo("認証状態を確認中", "checkAuthState")
            try {
                if (!repository.isAuthenticated()) {
                    AniiiiiictLogger.logInfo("未認証のため認証を開始", "checkAuthState")
                    startAuth()
                } else {
                    AniiiiiictLogger.logInfo("認証済みのためプログラム一覧を取得", "checkAuthState")
                    loadPrograms()
                }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "認証状態の確認中にエラーが発生")
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                loadPrograms()
                loadRecords()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    suspend fun loadPrograms() {
        repository.getProgramsWithWorks()
            .collect { programs ->
                _uiState.update { currentState ->
                    val filteredPrograms = applyFilters(programs, currentState.filterState)
                    updateAvailableFilters(programs)
                    currentState.copy(
                        programs = filteredPrograms,
                        isAuthenticating = false
                    )
                }
                preloadImages(_uiState.value.programs)
            }
    }

    private suspend fun loadRecords() {
        val result = repository.getRecords(null)
        _uiState.update { it.copy(records = result.records) }
    }

    private fun applyFilters(
        programs: List<ProgramWithWork>,
        filterState: FilterState
    ): List<ProgramWithWork> {
        var filtered = programs.filter { program ->
            (filterState.selectedMedia == null || program.work.media == filterState.selectedMedia) &&
                    (filterState.selectedSeason == null || program.work.seasonName?.split(" ")
                        ?.firstOrNull() == filterState.selectedSeason) &&
                    (filterState.selectedYear == null || program.work.seasonYear == filterState.selectedYear) &&
                    (filterState.selectedChannel == null || program.program.channel.name == filterState.selectedChannel) &&
                    (filterState.selectedStatus == null || program.work.viewerStatusState == filterState.selectedStatus.toString())
        }

        if (filterState.searchQuery.isNotBlank()) {
            filtered = filtered.filter { program ->
                program.work.title.contains(filterState.searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }

    private fun preloadImages(programs: List<ProgramWithWork>) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var successCount = 0
            var failCount = 0

            programs.forEach { program ->
                if (!currentCoroutineContext().isActive) return@forEach

                try {
                    val imageUrl = program.work.image?.recommendedImageUrl.takeIf {
                        !it.isNullOrEmpty() && it.startsWith(
                            "http",
                            ignoreCase = true
                        )
                    }
                        ?: program.work.image?.facebookOgImageUrl.takeIf {
                            !it.isNullOrEmpty() && it.startsWith(
                                "http",
                                ignoreCase = true
                            )
                        }

                    if (imageUrl == null) {
                        AniiiiiictLogger.logInfo(
                            "有効な画像URLがないためスキップ: ${program.work.title}",
                            "preloadImages"
                        )
                        return@forEach
                    }

                    val workId = try {
                        program.work.title.hashCode().toLong()
                    } catch (e: Exception) {
                        AniiiiiictLogger.logError(e, "ワークIDの変換に失敗: ${program.work.title}")
                        return@forEach
                    }

                    val success = repository.saveWorkImage(workId, imageUrl)
                    if (success) {
                        successCount++
                    } else {
                        failCount++
                        AniiiiiictLogger.logWarning(
                            "画像の保存に失敗: workId=$workId, url=$imageUrl",
                            "preloadImages"
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    AniiiiiictLogger.logError(
                        e,
                        "画像のプリロード中にエラー: ${program.work.title}"
                    )
                    failCount++
                }
            }

            val endTime = System.currentTimeMillis()
            AniiiiiictLogger.logInfo(
                "画像プリロード完了: 成功=${successCount}件, 失敗=${failCount}件, 合計時間=${endTime - startTime}ms",
                "preloadImages"
            )
        }
    }

    fun onImageLoad(workId: Int, imageUrl: String) {
        viewModelScope.launch {
            try {
                if (imageUrl.isBlank() || !imageUrl.startsWith("http", ignoreCase = true)) {
                    AniiiiiictLogger.logWarning("無効な画像URL: '$imageUrl'", "onImageLoad")
                    return@launch
                }

                val success = repository.saveWorkImage(workId.toLong(), imageUrl)
                if (!success) {
                    AniiiiiictLogger.logWarning("画像の保存に失敗 - workId: $workId", "onImageLoad")
                }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "画像の保存に失敗 - workId: $workId")
            }
        }
    }

    private fun startAuth() {
        if (isAuthInProgress) {
            println("MainViewModel: 認証処理がすでに進行中のため、新しい認証をスキップします")
            return
        }

        isAuthInProgress = true
        _uiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                AniiiiiictLogger.logInfo("認証URLを取得中", "startAuth")
                val authUrl = repository.getAuthUrl()
                AniiiiiictLogger.logInfo("認証URLを取得: $authUrl", "startAuth")

                delay(200)

                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(intent)
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "認証URLの取得に失敗")
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

    fun handleAuthCallback(code: String) {
        if (isAuthInProgress) {
            viewModelScope.launch {
                try {
                    AniiiiiictLogger.logInfo(
                        "認証コードを処理中: ${code.take(5)}...",
                        "handleAuthCallback"
                    )
                    println("MainViewModel: 認証コードを処理中: ${code.take(5)}...")

                    delay(200)

                    val success = repository.handleAuthCallback(code)
                    if (success) {
                        AniiiiiictLogger.logInfo(
                            "認証成功、プログラム一覧を読み込みます",
                            "handleAuthCallback"
                        )
                        println("MainViewModel: 認証成功")

                        delay(300)
                        _uiState.update { it.copy(isAuthenticating = false) }

                        loadPrograms()
                    } else {
                        AniiiiiictLogger.logWarning("認証が失敗しました", "handleAuthCallback")
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
                } catch (e: Exception) {
                    AniiiiiictLogger.logError(e, "認証コールバックの処理中にエラーが発生")
                    println("MainViewModel: 認証処理中に例外が発生 - ${e.message}")
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
        } else {
            println("MainViewModel: 認証処理が行われていないため、コールバック処理をスキップします")
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
        AniiiiiictLogger.logInfo("プログラム一覧を再読み込み", "MainViewModel.refresh")
        viewModelScope.launch {
            loadPrograms()
        }
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
                programs = applyFilters(currentState.programs, newFilterState)
            )
        }
        viewModelScope.launch {
            loadPrograms()
        }
    }

    private fun updateAvailableFilters(programs: List<ProgramWithWork>) {
        val media = programs.mapNotNull { it.work.media }.distinct().sorted()
        val seasons = programs.mapNotNull { it.work.seasonName }
            .map { it.split(" ").firstOrNull() ?: "" }
            .filter { it in listOf("SPRING", "SUMMER", "AUTUMN", "WINTER") }
            .distinct()
            .sorted()
        val years = programs.mapNotNull { it.work.seasonYear }
            .distinct()
            .sorted()
        val channels = programs.map { it.program.channel.name }.distinct().sorted()

        _uiState.update { currentState ->
            currentState.copy(
                availableMedia = media,
                availableSeasons = seasons,
                availableYears = years,
                availableChannels = channels
            )
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
        AniiiiiictLogger.logError(error, "MainViewModel")
        _uiState.update { it.copy(error = error.message) }
    }
}