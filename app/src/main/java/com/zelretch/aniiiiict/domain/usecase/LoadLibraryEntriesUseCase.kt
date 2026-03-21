package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.toLibraryEntry
import com.zelretch.aniiiiict.data.model.LibraryEntry
import timber.log.Timber
import javax.inject.Inject

class LoadLibraryEntriesUseCase @Inject constructor(
    private val libraryEntryDao: LibraryEntryDao
) {
    suspend operator fun invoke(): Result<List<LibraryEntry>> = try {
        val entries = libraryEntryDao.getAll().map { it.toLibraryEntry() }
        Timber.i("Roomからライブラリエントリーを読み込み: ${entries.size}件")
        Result.success(entries)
    } catch (e: Exception) {
        Timber.e(e, "ライブラリエントリーの読み込みに失敗")
        Result.failure(e)
    }
}
