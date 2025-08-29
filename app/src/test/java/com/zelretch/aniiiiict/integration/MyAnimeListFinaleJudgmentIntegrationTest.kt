package com.zelretch.aniiiiict.integration

import com.zelretch.aniiiiict.data.model.MyAnimeListMedia
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Integration test to verify the MyAnimeList finale judgment works end-to-end
 */
class MyAnimeListFinaleJudgmentIntegrationTest : BehaviorSpec({

    given("MyAnimeList finale judgment integration") {
        val repository = mockk<MyAnimeListRepository>()
        val useCase = JudgeFinaleUseCase(repository)

        `when`("anime is finished airing") {
            then("should confirm finale") {
                val media = MyAnimeListMedia(
                    id = 1,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "finished_airing",
                    broadcast = null
                )
                coEvery { repository.getMedia(1) } returns Result.success(media)
                
                val result = useCase(12, 1)
                
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("current episode equals total episodes") {
            then("should confirm finale") {
                val media = MyAnimeListMedia(
                    id = 2,
                    mediaType = "tv", 
                    numEpisodes = 24,
                    status = "currently_airing",
                    broadcast = null
                )
                coEvery { repository.getMedia(2) } returns Result.success(media)
                
                val result = useCase(24, 2)
                
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("anime is currently airing but not at final episode") {
            then("should not be finale") {
                val media = MyAnimeListMedia(
                    id = 3,
                    mediaType = "tv",
                    numEpisodes = 12, 
                    status = "currently_airing",
                    broadcast = null
                )
                coEvery { repository.getMedia(3) } returns Result.success(media)
                
                val result = useCase(8, 3)
                
                result.state shouldBe FinaleState.NOT_FINALE
                result.isFinale shouldBe false
            }
        }
    }
})