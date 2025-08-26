package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictAuthUseCase @Inject constructor(private val repository: AnnictRepository) {
    suspend fun isAuthenticated(): Boolean = repository.isAuthenticated()

    suspend fun getAuthUrl(): String = repository.getAuthUrl()

    suspend fun handleAuthCallback(code: String?): Boolean = if (code != null) {
        repository.handleAuthCallback(code)
    } else {
        false
    }
}
