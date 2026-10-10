package com.zelretch.aniiiiict.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.WorkPriority
import com.zelretch.aniiiiict.domain.sync.LibrarySyncService
import com.zelretch.aniiiiict.domain.sync.SyncStatus
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleJudgmentInfo
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadUnwatchedEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.SetWorkPriorityUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SEASON_SPRING = 0
private const val SEASON_SUMMER = 1
private const val SEASON_AUTUMN = 2
private const val SEASON_WINTER = 3

private val SEASON_ORDER = mapOf(
    SeasonName.SPRING to SEASON_SPRING,
    SeasonName.SUMMER to SEASON_SUMMER,
    SeasonName.AUTUMN to SEASON_AUTUMN,
    SeasonName.WINTER to SEASON_WINTER
)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

enum class LibrarySortOrder {
    TITLE_ASC,
    SEASON_DESC,
    SEASON_ASC
}

data class LibraryFilterState(
    val selectedMedia: Set<String> = emptySet(),
    val selectedStatuses: Set<StatusState> = emptySet(),
    val selectedYears: Set<Int> = emptySet(),
    val selectedSeasons: Set<SeasonName> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.SEASON_DESC
)

/** カードの「まとめて」で展開する未視聴エピソード一覧の状態 */
sealed interface BulkEpisodesState {
    data object Loading : BulkEpisodesState
    data class Loaded(val episodes: List<Episode>) : BulkEpisodesState
    data class Error(val message: String) : BulkEpisodesState
}

/** 最終話を記録したときに出す「視聴完了にしますか？」ダイアログの対象 */
data class FinaleConfirmation(
    val entryId: String,
    val workId: String,
    val episodeNumber: Int
)

data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val allEntries: List<LibraryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val filterState: LibraryFilterState = LibraryFilterState(),
    val availableMedia: List<String> = emptyList(),
    val availableStatuses: List<StatusState> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableSeasons: List<SeasonName> = emptyList(),
    val isFilterVisible: Boolean = false,
    val recordingEntryId: String? = null,
    // 「まとめて」を展開中のエントリー（同時に展開するのは1件だけ）
    val bulkRecordEntryId: String? = null,
    val bulkEpisodes: BulkEpisodesState? = null,
    val finaleConfirmation: FinaleConfirmation? = null
)

@HiltViewModel
@Suppress("TooManyFunctions", "LongParameterList")
class LibraryViewModel @Inject constructor(
    private val loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase,
    private val librarySyncService: LibrarySyncService,
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val loadUnwatchedEpisodesUseCase: LoadUnwatchedEpisodesUseCase,
    private val bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase,
    private val judgeFinaleUseCase: JudgeFinaleUseCase,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
    private val setWorkPriorityUseCase: SetWorkPriorityUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            librarySyncService.status.collect { status ->
                _uiState.update { it.copy(isSyncing = status is SyncStatus.Syncing) }
                if (status is SyncStatus.Idle) {
                    loadFromRoom()
                } else if (status is SyncStatus.Error) {
                    _uiState.update { it.copy(error = status.message) }
                }
            }
        }
        viewModelScope.launch {
            loadFromRoom()
        }
    }

    private suspend fun loadFromRoom() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadLibraryEntriesUseCase()
            .onSuccess { entries ->
                Timber.i("ライブラリエントリーを取得: ${entries.size}件")
                val availableMedia = entries.mapNotNull { it.work.media }.distinct().sorted()
                val availableStatuses = entries.mapNotNull { it.statusState }.distinct()
                val availableYears = entries.mapNotNull { it.work.seasonYear }.distinct().sortedDescending()
                val availableSeasons = entries.mapNotNull { it.work.seasonName }.distinct()
                    .sortedBy { SEASON_ORDER[it] }
                _uiState.update { currentState ->
                    currentState.copy(
                        allEntries = entries,
                        availableMedia = availableMedia,
                        availableStatuses = availableStatuses,
                        availableYears = availableYears,
                        availableSeasons = availableSeasons,
                        entries = applyFilters(entries, currentState.filterState),
                        isLoading = false,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                Timber.e(e, "ライブラリエントリーの読み込みに失敗")
                _uiState.update { it.copy(isLoading = false, error = errorMapper.toUserMessage(e)) }
            }
    }

    fun onEntryUpdated(libraryEntryId: String) {
        viewModelScope.launch {
            librarySyncService.syncEntry(libraryEntryId)
            loadFromRoom()
        }
    }

    /**
     * 詳細画面などで作品のステータスが変更されたとき、workId から該当エントリーを引いて再同期する。
     * ライブラリに存在しない作品（該当エントリー無し）の場合は何もしない。
     */
    fun onWorkStatusChanged(workId: String) {
        val entryId = _uiState.value.allEntries.firstOrNull { it.work.id == workId }?.id ?: return
        onEntryUpdated(entryId)
    }

    /**
     * 詳細画面などで優先度（端末内）が変わったとき、Room から読み直して並びに反映する。
     */
    fun onPriorityChanged() {
        viewModelScope.launch { loadFromRoom() }
    }

    /**
     * カードの「見た」ボタン：次の1話をその場で記録し、次の話を進めて進捗に反映する。
     */
    fun recordNextEpisode(entry: LibraryEntry) {
        val episode = entry.nextEpisode ?: return
        if (_uiState.value.recordingEntryId != null) return
        // 二重タップ防止: launch の外で同期的にフラグを立て、連打の2回目を確実に弾く
        _uiState.update { it.copy(recordingEntryId = entry.id, error = null) }
        viewModelScope.launch {
            watchEpisodeUseCase(
                episodeId = episode.id,
                workId = entry.work.id,
                currentStatus = entry.statusState ?: entry.work.viewerStatusState
            ).onSuccess {
                advanceAfterWatch(entry, episode)
                loadFromRoom()
                _uiState.update { it.copy(recordingEntryId = null) }
                judgeFinale(entry, episode.number)
            }.onFailure { e ->
                Timber.e(e, "「見た」記録に失敗: ${entry.work.title}")
                _uiState.update {
                    it.copy(
                        recordingEntryId = null,
                        error = errorMapper.toUserMessage(e, "LibraryViewModel.recordNextEpisode")
                    )
                }
            }
        }
    }

    /**
     * 「見た」のあと、作品のエピソード一覧から記録した話の次を引いて手元の行を進める。
     * 一覧が取れない・記録した話が先頭に無いなど辻褄が合わないときは Annict から取り直す。
     */
    private suspend fun advanceAfterWatch(entry: LibraryEntry, watched: Episode) {
        val episodes = loadUnwatchedEpisodesUseCase(entry.work.id, watched.id).getOrNull()
        if (episodes?.firstOrNull()?.id == watched.id) {
            librarySyncService.advanceEntry(entry.id, episodes.getOrNull(1))
        } else {
            librarySyncService.syncEntry(entry.id)
        }
    }

    /**
     * カードの「まとめて」：未視聴エピソード一覧の展開/折りたたみ。展開時に一覧を取得する。
     */
    fun toggleBulkRecord(entry: LibraryEntry) {
        if (_uiState.value.bulkRecordEntryId == entry.id) {
            collapseBulkRecord()
            return
        }
        _uiState.update { it.copy(bulkRecordEntryId = entry.id, bulkEpisodes = BulkEpisodesState.Loading) }
        viewModelScope.launch {
            val result = loadUnwatchedEpisodesUseCase(entry.work.id, entry.nextEpisode?.id)
            // 取得中に別のカードを開いた/閉じた場合は結果を捨てる
            if (_uiState.value.bulkRecordEntryId != entry.id) return@launch
            val state = result.fold(
                onSuccess = { BulkEpisodesState.Loaded(it) },
                onFailure = { e ->
                    BulkEpisodesState.Error(errorMapper.toUserMessage(e, "LibraryViewModel.toggleBulkRecord"))
                }
            )
            _uiState.update { it.copy(bulkEpisodes = state) }
        }
    }

    /**
     * 展開中の未視聴一覧の index 番目までをまとめて記録する。
     */
    fun bulkRecordUpTo(entry: LibraryEntry, upToIndex: Int) {
        val state = _uiState.value
        val episodes = (state.bulkEpisodes as? BulkEpisodesState.Loaded)?.episodes.orEmpty()
        val targets = episodes.take(upToIndex + 1)
        // 二重タップ防止: 記録中は弾く（launch の外で同期的にフラグを立てる）
        if (state.bulkRecordEntryId != entry.id || targets.isEmpty() || state.recordingEntryId != null) return
        _uiState.update { it.copy(recordingEntryId = entry.id, error = null) }
        viewModelScope.launch {
            val lastEpisode = targets.last()
            val result = bulkRecordEpisodesUseCase(
                episodeIds = targets.map { it.id },
                workId = entry.work.id,
                currentStatus = entry.statusState ?: entry.work.viewerStatusState,
                finaleInfo = entry.work.malAnimeId?.toIntOrNull()?.let { malId ->
                    FinaleJudgmentInfo(
                        malAnimeId = malId,
                        lastEpisodeNumber = lastEpisode.number,
                        lastEpisodeHasNext = lastEpisode.hasNextEpisode
                    )
                }
            )
            if (result.isSuccess) {
                // 開いている未視聴一覧から次の話がわかるので、Annict に取り直しに行かない
                librarySyncService.advanceEntry(entry.id, episodes.getOrNull(upToIndex + 1))
            } else {
                // 途中で失敗したときはどこまで記録できたかわからないので Annict から取り直す
                librarySyncService.syncEntry(entry.id)
            }
            loadFromRoom()
            collapseBulkRecord()
            _uiState.update { state ->
                state.copy(
                    recordingEntryId = null,
                    error = result.exceptionOrNull()?.let { e ->
                        Timber.e(e, "まとめて記録に失敗: ${entry.work.title}")
                        errorMapper.toUserMessage(e, "LibraryViewModel.bulkRecordUpTo")
                    }
                )
            }
            val episodeNumber = lastEpisode.number
            if (result.getOrNull()?.finaleResult?.isFinale == true && episodeNumber != null) {
                showFinaleConfirmation(entry, episodeNumber)
            }
        }
    }

    /**
     * 「見た」で記録した話が最終話かを MAL の話数で判定し、最終話ならダイアログを出す（Track と同じ基準）。
     */
    private suspend fun judgeFinale(entry: LibraryEntry, episodeNumber: Int?) {
        val malAnimeId = entry.work.malAnimeId?.toIntOrNull() ?: return
        if (episodeNumber == null) return
        if (judgeFinaleUseCase(episodeNumber, malAnimeId).isFinale) {
            showFinaleConfirmation(entry, episodeNumber)
        }
    }

    private fun showFinaleConfirmation(entry: LibraryEntry, episodeNumber: Int) {
        _uiState.update {
            it.copy(finaleConfirmation = FinaleConfirmation(entry.id, entry.work.id, episodeNumber))
        }
    }

    /**
     * 最終話ダイアログの「視聴完了にする」：作品を WATCHED にしてライブラリを再同期する。
     */
    fun confirmFinale() {
        val confirmation = _uiState.value.finaleConfirmation ?: return
        _uiState.update { it.copy(finaleConfirmation = null) }
        viewModelScope.launch {
            updateViewStateUseCase(confirmation.workId, StatusState.WATCHED)
                .onSuccess {
                    // 視聴完了はライブラリの対象外なので手元から消す
                    librarySyncService.removeEntry(confirmation.entryId)
                    loadFromRoom()
                }
                .onFailure { e ->
                    Timber.e(e, "視聴完了への変更に失敗: workId=${confirmation.workId}")
                    _uiState.update {
                        it.copy(error = errorMapper.toUserMessage(e, "LibraryViewModel.confirmFinale"))
                    }
                }
        }
    }

    fun dismissFinale() {
        _uiState.update { it.copy(finaleConfirmation = null) }
    }

    /**
     * カード長押しから優先度（Tier1/Tier2/Tier3/無印）を変更する。端末内に保存し、並びに反映する。
     */
    fun setPriority(entry: LibraryEntry, priority: WorkPriority) {
        if (entry.priority == priority) return
        viewModelScope.launch {
            setWorkPriorityUseCase(entry.work.id, priority)
                .onSuccess { loadFromRoom() }
                .onFailure { e ->
                    Timber.e(e, "優先度の変更に失敗: ${entry.work.title}")
                    _uiState.update {
                        it.copy(error = errorMapper.toUserMessage(e, "LibraryViewModel.setPriority"))
                    }
                }
        }
    }

    private fun collapseBulkRecord() {
        _uiState.update { it.copy(bulkRecordEntryId = null, bulkEpisodes = null) }
    }

    fun toggleMediaFilter(media: String) = updateFilter { it.copy(selectedMedia = it.selectedMedia.toggle(media)) }

    fun toggleStatusFilter(status: StatusState) =
        updateFilter { it.copy(selectedStatuses = it.selectedStatuses.toggle(status)) }

    fun toggleYearFilter(year: Int) = updateFilter { it.copy(selectedYears = it.selectedYears.toggle(year)) }

    fun toggleSeasonFilter(season: SeasonName) =
        updateFilter { it.copy(selectedSeasons = it.selectedSeasons.toggle(season)) }

    fun updateSearchQuery(query: String) = updateFilter { it.copy(searchQuery = query) }

    fun updateSortOrder(sortOrder: LibrarySortOrder) = updateFilter { it.copy(sortOrder = sortOrder) }

    fun toggleFilterVisibility() {
        _uiState.update { it.copy(isFilterVisible = !it.isFilterVisible) }
    }

    private fun updateFilter(transform: (LibraryFilterState) -> LibraryFilterState) {
        _uiState.update { currentState ->
            val newFilterState = transform(currentState.filterState)
            currentState.copy(
                filterState = newFilterState,
                entries = applyFilters(currentState.allEntries, newFilterState)
            )
        }
    }

    @Suppress("ComplexMethod")
    private fun applyFilters(entries: List<LibraryEntry>, filterState: LibraryFilterState): List<LibraryEntry> {
        var filtered = entries
        if (filterState.selectedMedia.isNotEmpty()) {
            filtered = filtered.filter { it.work.media in filterState.selectedMedia }
        }
        if (filterState.selectedStatuses.isNotEmpty()) {
            filtered = filtered.filter { it.statusState in filterState.selectedStatuses }
        }
        if (filterState.selectedYears.isNotEmpty()) {
            filtered = filtered.filter { it.work.seasonYear in filterState.selectedYears }
        }
        if (filterState.selectedSeasons.isNotEmpty()) {
            filtered = filtered.filter { it.work.seasonName in filterState.selectedSeasons }
        }
        val searched = if (filterState.searchQuery.isBlank()) {
            filtered
        } else {
            val query = filterState.searchQuery.trim().lowercase()
            filtered.filter { it.work.title.lowercase().contains(query) }
        }
        // 優先度でセクション分けする。sortedBy は安定ソートなので、セクション内は選んだ並び順のまま
        return sortEntries(searched, filterState.sortOrder).sortedBy { it.priority.ordinal }
    }

    private fun sortEntries(entries: List<LibraryEntry>, sortOrder: LibrarySortOrder): List<LibraryEntry> =
        when (sortOrder) {
            LibrarySortOrder.TITLE_ASC -> entries.sortedBy { it.work.title }
            LibrarySortOrder.SEASON_DESC -> entries.sortedWith(
                compareByDescending<LibraryEntry> { it.work.seasonYear }
                    .thenByDescending { SEASON_ORDER[it.work.seasonName] }
            )
            LibrarySortOrder.SEASON_ASC -> entries.sortedWith(
                compareBy<LibraryEntry> { it.work.seasonYear }
                    .thenBy { SEASON_ORDER[it.work.seasonName] }
            )
        }
}
