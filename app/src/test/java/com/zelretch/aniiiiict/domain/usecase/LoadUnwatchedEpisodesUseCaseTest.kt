package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LoadUnwatchedEpisodesUseCase")
class LoadUnwatchedEpisodesUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: LoadUnwatchedEpisodesUseCase

    private val episodes = listOf(
        Episode(id = "ep1", number = 1, viewerDidTrack = true),
        Episode(id = "ep2", number = 2, viewerDidTrack = false),
        Episode(id = "ep3", number = 3, viewerDidTrack = false)
    )

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = LoadUnwatchedEpisodesUseCase(repository)
        coEvery { repository.getWorkEpisodes("work1") } returns Result.success(episodes)
    }

    @Test
    @DisplayName("nextEpisode以降を未視聴として返す")
    fun returnsEpisodesFromNextEpisode() = runTest {
        val result = useCase("work1", "ep2").getOrThrow()

        assertEquals(listOf("ep2", "ep3"), result.map { it.id })
    }

    @Test
    @DisplayName("最後の話以外は次の話ありとして返す")
    fun setsHasNextEpisode() = runTest {
        val result = useCase("work1", "ep2").getOrThrow()

        assertEquals(listOf(true, false), result.map { it.hasNextEpisode })
    }

    @Test
    @DisplayName("nextEpisodeが一覧に無い場合は未記録の話にフォールバックする")
    fun fallsBackToViewerDidTrack() = runTest {
        val result = useCase("work1", null).getOrThrow()

        assertEquals(listOf("ep2", "ep3"), result.map { it.id })
    }

    @Test
    @DisplayName("取得に失敗した場合は失敗を返す")
    fun propagatesFailure() = runTest {
        coEvery { repository.getWorkEpisodes("work1") } returns Result.failure(RuntimeException("boom"))

        assertTrue(useCase("work1", "ep2").isFailure)
    }
}
