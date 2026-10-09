package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import com.zelretch.aniiiiict.data.local.toLibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.WorkPriority
import timber.log.Timber
import javax.inject.Inject

class LoadLibraryEntriesUseCase @Inject constructor(
    private val libraryEntryDao: LibraryEntryDao,
    private val workPriorityDao: WorkPriorityDao
) {
    suspend operator fun invoke(): Result<List<LibraryEntry>> = try {
        val priorities = workPriorityDao.getAll().associate { it.workId to it.priority }
        val entries = libraryEntryDao.getAll().map { entity ->
            val priority = priorities[entity.workId]
                ?.let { runCatching { WorkPriority.valueOf(it) }.getOrNull() }
                ?: WorkPriority.NONE
            entity.toLibraryEntry().copy(priority = priority)
        }
        Timber.i("Roomからライブラリエントリーを読み込み: ${entries.size}件")
        Result.success(entries)
    } catch (e: Exception) {
        Timber.e(e, "ライブラリエントリーの読み込みに失敗")
        Result.failure(e)
    }
}
