package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse

interface MyAnimeListRepository {
    suspend fun getMedia(mediaId: Int): Result<MyAnimeListResponse>
}
