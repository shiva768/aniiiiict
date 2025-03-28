package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.AnnictProgram
import com.zelretch.aniiiiiict.data.model.AnnictWork
import java.time.LocalDate

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String)
    suspend fun getWorks(): List<AnnictWork>
    suspend fun getPrograms(
        unwatched: Boolean = false
    ): List<AnnictProgram>
    suspend fun createRecord(episodeId: Long)
    suspend fun getCustomStartDate(workId: Long): LocalDate?
    suspend fun setCustomStartDate(workId: Long, date: LocalDate)
    suspend fun clearCustomStartDate(workId: Long)
    suspend fun saveWorkImage(workId: Long, imageUrl: String)
    suspend fun getWorkImage(workId: Long): WorkImage?
} 