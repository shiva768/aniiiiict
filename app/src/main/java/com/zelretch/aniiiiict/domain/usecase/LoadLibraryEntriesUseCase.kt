package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.toEntity
import com.zelretch.aniiiiict.data.local.toLibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryFetchParams
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import timber.log.Timber
import javax.inject.Inject

class LoadLibraryEntriesUseCase @Inject constructor(
    private val repository: AnnictRepository,
    private val libraryEntryDao: LibraryEntryDao
) {
    suspend operator fun invoke(params: LibraryFetchParams, forceRefresh: Boolean = false): Result<List<LibraryEntry>> {
        val hash = params.hash
        if (!forceRefresh) {
            val cached = libraryEntryDao.getByHash(hash)
            if (cached.isNotEmpty()) {
                Timber.i("キャッシュヒット: hash=$hash, ${cached.size}件")
                return Result.success(cached.map { it.toLibraryEntry() })
            }
        }
        Timber.i("APIからフェッチ開始: hash=$hash, forceRefresh=$forceRefresh")
        return fetchAllPages(params).onSuccess { entries ->
            libraryEntryDao.deleteByHash(hash)
            libraryEntryDao.insertAll(entries.map { it.toEntity(hash) })
            Timber.i("Roomに保存完了: ${entries.size}件")
        }
    }

    private suspend fun fetchAllPages(params: LibraryFetchParams): Result<List<LibraryEntry>> {
        val allEntries = mutableListOf<LibraryEntry>()
        var cursor: String? = null
        var hasNextPage = true
        while (hasNextPage) {
            val result = repository.getLibraryEntries(params.selectedStates, params.seasonFrom, cursor)
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
