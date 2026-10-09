package com.zelretch.aniiiiict.domain.sync

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.LibraryEntryEntity
import com.zelretch.aniiiiict.data.local.toEntity
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Error(val message: String) : SyncStatus
}

@Singleton
class LibrarySyncService @Inject constructor(
    private val repository: AnnictRepository,
    private val libraryEntryDao: LibraryEntryDao
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val targetStates = listOf(StatusState.WANNA_WATCH, StatusState.WATCHING, StatusState.ON_HOLD)

    suspend fun sync() {
        if (_status.value is SyncStatus.Syncing) {
            Timber.w("既に同期中のためスキップ")
            return
        }
        _status.value = SyncStatus.Syncing
        Timber.i("ライブラリ同期開始")

        val result = fetchAllPages()
        result
            .onSuccess { entries ->
                libraryEntryDao.replaceAll(entries.map { it.toEntity() })
                Timber.i("ライブラリ同期完了: ${entries.size}件")
                _status.value = SyncStatus.Idle
            }
            .onFailure { e ->
                Timber.e(e, "ライブラリ同期失敗")
                _status.value = SyncStatus.Error(e.message ?: "同期に失敗しました")
            }
    }

    /**
     * 1件だけ Annict と同期する。
     * node(id:) で引くとエントリーが null になり消えてしまうことがあったので、
     * 全件同期と同じ一覧クエリを作品のシーズンで絞って探す。
     * 見つからない（視聴完了にした・シーズン不明など）ときは、消す判断を全件同期に任せる。
     */
    suspend fun syncEntry(libraryEntryId: String) {
        Timber.i("エントリー更新: id=$libraryEntryId")
        val season = libraryEntryDao.getById(libraryEntryId)?.annictSeason()
        if (season == null) {
            Timber.i("シーズン不明のため全件同期: id=$libraryEntryId")
            sync()
            return
        }
        fetchAllPages(seasons = listOf(season))
            .onSuccess { entries ->
                val entry = entries.firstOrNull { it.id == libraryEntryId }
                if (entry != null) {
                    libraryEntryDao.upsert(entry.toEntity())
                    Timber.i("エントリー更新完了: id=$libraryEntryId")
                } else {
                    Timber.i("シーズン内に見つからないため全件同期: id=$libraryEntryId, season=$season")
                    sync()
                }
            }
            .onFailure { e ->
                Timber.e(e, "エントリー更新失敗: id=$libraryEntryId")
            }
    }

    private suspend fun fetchAllPages(
        seasons: List<String>? = null
    ): Result<List<com.zelretch.aniiiiict.data.model.LibraryEntry>> {
        val allEntries = mutableListOf<com.zelretch.aniiiiict.data.model.LibraryEntry>()
        var cursor: String? = null
        var hasNextPage = true
        while (hasNextPage) {
            val result = repository.getLibraryEntries(targetStates, cursor, seasons)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val page = result.getOrThrow()
            allEntries.addAll(page.entries)
            hasNextPage = page.hasNextPage
            cursor = page.endCursor
            Timber.i("ページ取得: ${page.entries.size}件, hasNextPage=$hasNextPage")
        }
        return Result.success(allEntries)
    }
}

/** Annict の seasons 引数の形式（例: "2024-autumn"） */
private fun LibraryEntryEntity.annictSeason(): String? {
    val year = workSeasonYear ?: return null
    val name = workSeasonName ?: return null
    return "$year-${name.lowercase()}"
}
