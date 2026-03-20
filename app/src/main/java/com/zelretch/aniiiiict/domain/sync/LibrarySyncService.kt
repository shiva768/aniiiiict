package com.zelretch.aniiiiict.domain.sync

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
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

    private val targetStates = listOf(StatusState.WANNA_WATCH, StatusState.WATCHING)

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

    suspend fun syncEntry(libraryEntryId: String) {
        Timber.i("エントリー更新: id=$libraryEntryId")
        repository.getLibraryEntry(libraryEntryId)
            .onSuccess { entry ->
                if (entry == null) {
                    libraryEntryDao.deleteById(libraryEntryId)
                    Timber.i("エントリーが見つからないため削除: id=$libraryEntryId")
                    return
                }
                val status = entry.statusState ?: entry.work.viewerStatusState
                if (status in targetStates) {
                    libraryEntryDao.upsert(entry.toEntity())
                    Timber.i("エントリー更新完了: id=$libraryEntryId")
                } else {
                    libraryEntryDao.deleteById(libraryEntryId)
                    Timber.i("対象外ステータスのため削除: id=$libraryEntryId, status=$status")
                }
            }
            .onFailure { e ->
                Timber.e(e, "エントリー更新失敗: id=$libraryEntryId")
            }
    }

    private suspend fun fetchAllPages(): Result<List<com.zelretch.aniiiiict.data.model.LibraryEntry>> {
        val allEntries = mutableListOf<com.zelretch.aniiiiict.data.model.LibraryEntry>()
        var cursor: String? = null
        var hasNextPage = true
        while (hasNextPage) {
            val result = repository.getLibraryEntries(targetStates, cursor)
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
