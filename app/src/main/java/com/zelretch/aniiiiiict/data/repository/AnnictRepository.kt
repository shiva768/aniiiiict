package com.zelretch.aniiiiiict.data.repository

import com.annict.ViewerProgramsQuery
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiiict.data.model.Record
import kotlinx.coroutines.flow.Flow

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String): Boolean
    suspend fun createRecord(episodeId: String, workId: String): Boolean
    suspend fun getRawProgramsData(): Flow<List<ViewerProgramsQuery.Node?>>
    suspend fun getRecords(after: String? = null): PaginatedRecords
    suspend fun getAllRecords(): List<Record>
    suspend fun deleteRecord(recordId: String): Boolean
    suspend fun updateWorkViewStatus(workId: String, state: StatusState): Boolean
}
