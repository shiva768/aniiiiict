package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.type.StatusState
import kotlinx.coroutines.flow.Flow

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String): Boolean
    suspend fun getWorks(): List<AnnictWork>
    suspend fun createRecord(episodeId: String, workId: String): Boolean
    suspend fun saveWorkImage(workId: Long, imageUrl: String): Boolean
    suspend fun getWorkImage(workId: Long): WorkImage?
    suspend fun getProgramsWithWorks(): Flow<List<ProgramWithWork>>
    suspend fun getRecords(limit: Int = 30): List<Record>
    suspend fun deleteRecord(recordId: String): Boolean
    suspend fun updateWorkStatus(workId: String, state: StatusState): Boolean
} 