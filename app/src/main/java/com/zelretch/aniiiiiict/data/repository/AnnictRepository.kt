package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.type.StatusState
import kotlinx.coroutines.flow.Flow

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String): Boolean
    suspend fun createRecord(episodeId: String, workId: String): Boolean
    fun getProgramsWithWorks(): Flow<List<ProgramWithWork>>
    suspend fun getRecords(after: String? = null): PaginatedRecords
    suspend fun deleteRecord(recordId: String): Boolean
    suspend fun updateWorkViewStatus(workId: String, state: StatusState): Boolean
} 