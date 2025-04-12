package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
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


    private var loadProgramsJob: kotlinx.coroutines.Job? = null
    private var isAuthInProgress = false

    init {
        checkAuthState()
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
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証状態の確認に失敗しました",
                    isLoading = false
                )
            }
        }
    }

    fun loadPrograms() {
        loadProgramsJob?.cancel()
        loadProgramsJob = viewModelScope.launch {
            try {
                if (!currentCoroutineContext().isActive) return@launch

                // データがすでに表示されている場合はローディングを表示しない
                if (_uiState.value.programs.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }

                // 少し遅延させてUIの更新頻度を下げる
                delay(300)
                if (!currentCoroutineContext().isActive) return@launch

                val filterState = _uiState.value.filterState
                repository.getProgramsWithWorks().collect { programs ->
                    var filteredPrograms = programs.filter { program ->
                        (filterState.selectedMedia == null || program.work.media == filterState.selectedMedia) &&
                        (filterState.selectedSeason == null || program.work.seasonName?.split(" ")?.firstOrNull() == filterState.selectedSeason) &&
                        (filterState.selectedYear == null || program.work.seasonYear == filterState.selectedYear) &&
                        (filterState.selectedChannel == null || program.program.channel.name == filterState.selectedChannel) &&
                        (filterState.selectedStatus == null || program.work.viewerStatusState == filterState.selectedStatus.toString())
                    }

                    // 検索クエリでフィルタリング
                    if (filterState.searchQuery.isNotBlank()) {
                        filteredPrograms = filteredPrograms.filter { program ->
                            program.work.title.contains(filterState.searchQuery, ignoreCase = true)
                        }
                    }

                    updateAvailableFilters(filteredPrograms)
                    _uiState.value = _uiState.value.copy(
                        programs = filteredPrograms,
                        isLoading = false,
                        isAuthenticating = false
                    )

                    // バックグラウンドで画像をプリロード
                    preloadImages(filteredPrograms)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AniiiiiictLogger.logError(e, "プログラム一覧の取得に失敗")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "プログラム一覧の取得に失敗しました",
                    isLoading = false
                )
            }
        }
    }

    /**
     * 画像を非同期でプリロードする
     * UI表示を遅延させずにバックグラウンドで画像を取得・保存
     */
    private fun preloadImages(programs: List<ProgramWithWork>) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var successCount = 0
            var failCount = 0

            programs.forEach { program ->
                if (!isActive) return@forEach

                try {
                    // 画像URLの取得
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
                        // 有効なURLがない場合はスキップ
                        AniiiiiictLogger.logInfo(
                            "有効な画像URLがないためスキップ: ${program.work.title}",
                            "preloadImages"
                        )
                        return@forEach
                    }

                    // ワークIDの取得を試みる
                    val workId = try {
                        program.work.title.hashCode().toLong() // 一時的な解決策としてハッシュコードを使用
                    } catch (e: Exception) {
                        AniiiiiictLogger.logError(e, "ワークIDの変換に失敗: ${program.work.title}")
                        return@forEach
                    }

                    // 画像の保存処理
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
                // 無効なURLはスキップ
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
        _uiState.value = _uiState.value.copy(isAuthenticating = true)

        viewModelScope.launch {
            try {
                AniiiiiictLogger.logInfo("認証URLを取得中", "startAuth")
                val authUrl = repository.getAuthUrl()
                AniiiiiictLogger.logInfo("認証URLを取得: $authUrl", "startAuth")

                // 少し遅延を入れて遷移を安定させる
                delay(200)

                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(intent)
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "認証URLの取得に失敗")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証に失敗しました",
                    isLoading = false,
                    isAuthenticating = false
                )
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

                    // 少し遅延を入れて画面遷移を安定させる
                    delay(200)

                    val success = repository.handleAuthCallback(code)
                    if (success) {
                        AniiiiiictLogger.logInfo(
                            "認証成功、プログラム一覧を読み込みます",
                            "handleAuthCallback"
                        )
                        println("MainViewModel: 認証成功")

                        // 処理完了後に状態をリセット
                        isAuthInProgress = false

                        // 認証に成功したら、少し待ってからUIを更新
                        delay(300)
                        _uiState.value = _uiState.value.copy(isAuthenticating = false)

                        // プログラム一覧を読み込む
                        loadPrograms()
                    } else {
                        AniiiiiictLogger.logWarning("認証が失敗しました", "handleAuthCallback")
                        println("MainViewModel: 認証失敗")

                        // 少し待ってからUIを更新
                        delay(200)
                        _uiState.value = _uiState.value.copy(
                            error = "認証に失敗しました。再度お試しください。",
                            isLoading = false,
                            isAuthenticating = false
                        )
                        isAuthInProgress = false
                    }
                } catch (e: Exception) {
                    AniiiiiictLogger.logError(e, "認証コールバックの処理中にエラーが発生")
                    println("MainViewModel: 認証処理中に例外が発生 - ${e.message}")
                    e.printStackTrace()

                    // 少し待ってからUIを更新
                    delay(200)
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "認証に失敗しました",
                        isLoading = false,
                        isAuthenticating = false
                    )
                    isAuthInProgress = false
                }
            }
        } else {
            println("MainViewModel: 認証処理が行われていないため、コールバック処理をスキップします")
        }
    }

    /**
     * 認証をキャンセルする
     * ブラウザから戻ってきた時に認証コードがない場合などに呼び出される
     */
    fun cancelAuth() {
        viewModelScope.launch {
            println("MainViewModel: 認証をキャンセルします")
            // 状態をリセット
            isAuthInProgress = false
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                isLoading = false
            )
        }
    }

    /**
     * エピソードを視聴済みとして記録する
     */
    fun recordEpisode(episodeId: String, workId: String, currentStatus: StatusState) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRecording = true)

                // ユースケースを実行
                watchEpisodeUseCase(episodeId, workId, currentStatus)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isRecording = false,
                            recordingSuccess = episodeId,
                            error = null
                        )

                        // 成功メッセージをリセット
                        delay(2000)
                        if (_uiState.value.recordingSuccess == episodeId) {
                            _uiState.value = _uiState.value.copy(recordingSuccess = null)
                        }

                        // プログラム一覧を再読込
                        loadPrograms()
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isRecording = false,
                            error = e.message ?: "エピソードの記録に失敗しました"
                        )
                    }
            } catch (e: Exception) {
                AniiiiiictLogger.logError(e, "エピソードの記録に失敗: episodeId=$episodeId")
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    error = "エピソードの記録に失敗しました: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * プログラム一覧を再読み込み
     */
    fun refresh() {
        AniiiiiictLogger.logInfo("プログラム一覧を再読み込み", "MainViewModel.refresh")
        loadPrograms()
    }

    fun updateFilter(
        selectedMedia: String? = _uiState.value.filterState.selectedMedia,
        selectedSeason: String? = _uiState.value.filterState.selectedSeason,
        selectedYear: Int? = _uiState.value.filterState.selectedYear,
        selectedChannel: String? = _uiState.value.filterState.selectedChannel,
        selectedStatus: StatusState? = _uiState.value.filterState.selectedStatus,
        searchQuery: String = _uiState.value.filterState.searchQuery
    ) {
        _uiState.value = _uiState.value.copy(
            filterState = FilterState(
                selectedMedia = selectedMedia,
                selectedSeason = selectedSeason,
                selectedYear = selectedYear,
                selectedChannel = selectedChannel,
                selectedStatus = selectedStatus,
                searchQuery = searchQuery
            )
        )
        loadPrograms()
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

        _uiState.value = _uiState.value.copy(
            availableMedia = media,
            availableSeasons = seasons,
            availableYears = years,
            availableChannels = channels
        )
    }

    fun toggleFilterVisibility() {
        _uiState.value = _uiState.value.copy(
            isFilterVisible = !_uiState.value.isFilterVisible
        )
    }
}