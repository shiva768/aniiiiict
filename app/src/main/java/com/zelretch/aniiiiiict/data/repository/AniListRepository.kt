package com.zelretch.aniiiiiict.data.repository

import com.zelretch.aniiiiiict.data.model.AniListMedia

interface AniListRepository {
    suspend fun getMedia(mediaId: Int): Result<AniListMedia>
}
