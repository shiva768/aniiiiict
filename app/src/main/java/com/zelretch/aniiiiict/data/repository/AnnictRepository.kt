package com.zelretch.aniiiiict.data.repository

import com.annict.ViewerProgramsQuery
import com.annict.WorkDetailQuery
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.PaginatedRecords

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String): Result<Unit>
    suspend fun createRecord(episodeId: String, workId: String): Result<Unit>
    suspend fun getRawProgramsData(): Result<List<ViewerProgramsQuery.Node?>>
    suspend fun getRecords(after: String? = null): Result<PaginatedRecords>
    suspend fun deleteRecord(recordId: String): Result<Unit>
    suspend fun updateWorkViewStatus(workId: String, state: StatusState): Result<Unit>
    suspend fun getWorkDetail(workId: String): Result<WorkDetailQuery.Node?>
    suspend fun getLibraryEntries(states: List<StatusState>, after: String? = null): Result<List<LibraryEntry>>
}
