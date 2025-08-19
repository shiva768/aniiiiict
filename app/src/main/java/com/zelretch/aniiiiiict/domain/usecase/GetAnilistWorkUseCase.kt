package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import javax.inject.Inject

class GetAnilistWorkUseCase @Inject constructor(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(malAnimeId: String?): Result<AniListMedia> {
        if (malAnimeId == null) {
            return Result.failure(IllegalArgumentException("malAnimeId is null"))
        }

        val malId = malAnimeId.toIntOrNull()
        if (malId == null) {
            return Result.failure(IllegalArgumentException("malAnimeId is not a valid integer"))
        }

        return aniListRepository.getMediaByMalId(malId)
    }
}
