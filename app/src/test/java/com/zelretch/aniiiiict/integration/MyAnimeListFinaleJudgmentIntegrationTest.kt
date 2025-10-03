package com.zelretch.aniiiiict.integration

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
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

    given("MyAnimeList最終話判定統合テスト") {
        val repository = mockk<MyAnimeListRepository>()
        val useCase = JudgeFinaleUseCase(repository)

        `when`("アニメが放送終了している") {
            then("最終話として確認される") {
                val media = MyAnimeListResponse(
                    id = 1,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "finished_airing",
                    broadcast = null,
                    mainPicture = null
                )
                coEvery { repository.getAnimeDetail(1) } returns Result.success(media)

                val result = useCase(12, 1)

                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("現在のエピソード数が全エピソード数に等しい") {
            then("最終話として確認される") {
                val media = MyAnimeListResponse(
                    id = 2,
                    mediaType = "tv",
                    numEpisodes = 24,
                    status = "currently_airing",
                    broadcast = null,
                    mainPicture = null
                )
                coEvery { repository.getAnimeDetail(2) } returns Result.success(media)

                val result = useCase(24, 2)

                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("アニメが現在放送中だが最終話ではない") {
            then("最終話ではない") {
                val media = MyAnimeListResponse(
                    id = 3,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "currently_airing",
                    broadcast = null,
                    mainPicture = null
                )
                coEvery { repository.getAnimeDetail(3) } returns Result.success(media)

                val result = useCase(8, 3)

                result.state shouldBe FinaleState.NOT_FINALE
                result.isFinale shouldBe false
            }
        }
    }
})
