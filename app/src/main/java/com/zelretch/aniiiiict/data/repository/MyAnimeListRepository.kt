package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.data.model.MyAnimeListMedia

interface MyAnimeListRepository {
    suspend fun getMedia(mediaId: Int): Result<MyAnimeListMedia>
}
