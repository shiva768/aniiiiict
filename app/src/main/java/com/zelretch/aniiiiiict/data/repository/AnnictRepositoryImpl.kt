package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.dao.CustomStartDateDao
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate
import com.zelretch.aniiiiiict.data.model.AnnictWork
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val apiClient: AnnictApiClient,
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val customStartDateDao: CustomStartDateDao
) : AnnictRepository {
    override suspend fun isAuthenticated(): Boolean {
        return tokenManager.hasValidToken()
    }

    override suspend fun getAuthUrl(): String {
        return authManager.getAuthorizationUrl()
    }

    override suspend fun getWorks(): List<AnnictWork> {
        val watchingWorks = apiClient.getWatchingWorks().getOrThrow()
        val wantToWatchWorks = apiClient.getWantToWatchWorks().getOrThrow()
        return watchingWorks + wantToWatchWorks
    }

    override suspend fun getCustomStartDate(workId: Long): LocalDateTime? {
        return customStartDateDao.getCustomStartDate(workId).first()?.startDate
    }

    override suspend fun setCustomStartDate(workId: Long, date: LocalDateTime) {
        customStartDateDao.setCustomStartDate(CustomStartDate(workId, date))
    }

    override suspend fun clearCustomStartDate(workId: Long) {
        customStartDateDao.deleteCustomStartDate(workId)
    }

    override suspend fun handleAuthCallback(code: String) {
        authManager.handleAuthorizationCode(code).getOrThrow()
    }
} 