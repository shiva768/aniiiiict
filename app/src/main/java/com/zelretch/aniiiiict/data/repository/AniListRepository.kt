package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.data.model.AniListMedia

interface AniListRepository {
    suspend fun getMedia(mediaId: Int): Result<AniListMedia>
}
