package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Record
import kotlinx.coroutines.flow.Flow

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String): Boolean
    suspend fun getWorks(): List<AnnictWork>
    suspend fun getPrograms(
        unwatched: Boolean = false
    ): List<ProgramWithWork>
    suspend fun createRecord(episodeId: String): Boolean
    suspend fun saveWorkImage(workId: Long, imageUrl: String): Boolean
    suspend fun getWorkImage(workId: Long): WorkImage?
    suspend fun getProgramsWithWorks(): Flow<List<ProgramWithWork>>
    suspend fun getRecords(limit: Int = 30): List<Record>
    suspend fun deleteRecord(recordId: String): Boolean
} 