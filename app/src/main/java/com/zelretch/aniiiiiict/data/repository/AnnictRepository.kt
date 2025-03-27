package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.model.AnnictWork
import java.time.LocalDateTime

interface AnnictRepository {
    suspend fun isAuthenticated(): Boolean
    suspend fun getAuthUrl(): String
    suspend fun handleAuthCallback(code: String)
    suspend fun getWorks(): List<AnnictWork>
    suspend fun getCustomStartDate(workId: Long): LocalDateTime?
    suspend fun setCustomStartDate(workId: Long, date: LocalDateTime)
    suspend fun clearCustomStartDate(workId: Long)
} 