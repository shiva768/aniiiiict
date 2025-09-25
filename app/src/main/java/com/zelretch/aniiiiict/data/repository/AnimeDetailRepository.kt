package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.data.model.AnimeDetailInfo

interface AnimeDetailRepository {
    suspend fun getAnimeDetailInfo(workId: String, malAnimeId: String?): Result<AnimeDetailInfo>
}