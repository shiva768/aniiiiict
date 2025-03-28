package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.dao.CustomStartDateDao
import com.zelretch.aniiiiiict.data.local.dao.WorkImageDao
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate
import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.AnnictProgram
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.util.ImageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val apiClient: AnnictApiClient,
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val customStartDateDao: CustomStartDateDao,
    private val workImageDao: WorkImageDao,
    private val imageDownloader: ImageDownloader
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

    override suspend fun getPrograms(
        unwatched: Boolean
    ): List<AnnictProgram> {
        return apiClient.getPrograms(
            unwatched = unwatched
        ).getOrThrow()
    }

    override suspend fun createRecord(episodeId: Long) {
        apiClient.createRecord(episodeId).getOrThrow()
    }

    override suspend fun getCustomStartDate(workId: Long): LocalDate? {
        return customStartDateDao.getCustomStartDate(workId).first()?.startDate
    }

    override suspend fun setCustomStartDate(workId: Long, date: LocalDate) {
        customStartDateDao.setCustomStartDate(CustomStartDate(workId, date))
    }

    override suspend fun clearCustomStartDate(workId: Long) {
        customStartDateDao.deleteCustomStartDate(workId)
    }

    override suspend fun handleAuthCallback(code: String) {
        authManager.handleAuthorizationCode(code).getOrThrow()
    }

    override suspend fun saveWorkImage(workId: Long, imageUrl: String) {
        withContext(Dispatchers.IO) {
            val localPath = imageDownloader.downloadImage(workId, imageUrl)
            workImageDao.insertWorkImage(WorkImage(workId, imageUrl, localPath))
        }
    }

    override suspend fun getWorkImage(workId: Long): WorkImage? {
        return workImageDao.getWorkImage(workId).firstOrNull()
    }
} 