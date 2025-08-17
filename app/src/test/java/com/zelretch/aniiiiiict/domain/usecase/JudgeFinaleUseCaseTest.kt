package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class JudgeFinaleUseCaseTest : BehaviorSpec({

    val aniListRepository = mockk<AniListRepository>()
    val judgeFinaleUseCase = JudgeFinaleUseCase(aniListRepository)

    given("最終話判定ロジック") {
        `when`("次回予定ありかつ nextAiringEpisode.episode > currentEp の場合") {
            then("not_finale を返す") {
                val media = AniListMedia(
                    id = 1,
                    format = "TV",
                    episodes = 12,
                    status = "RELEASING",
                    nextAiringEpisode = NextAiringEpisode(episode = 11, airingAt = 1678886400) // 次回が現在のエピソードより大きい
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.NOT_FINALE
                result.isFinale shouldBe false
            }
        }

        `when`("episodes が数値 かつ currentEp >= episodes かつ nextAiringEpisode == null の場合") {
            then("finale_confirmed を返す") {
                val media = AniListMedia(
                    id = 2,
                    format = "TV",
                    episodes = 12,
                    status = "RELEASING",
                    nextAiringEpisode = null
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(12, media.id)
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("status == FINISHED かつ nextAiringEpisode == null の場合") {
            then("finale_confirmed を返す") {
                val media = AniListMedia(
                    id = 3,
                    format = "TV",
                    episodes = null,
                    status = "FINISHED",
                    nextAiringEpisode = null
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("nextAiringEpisode == null（ただし 2,3 未満足）の場合") {
            then("finale_expected を返す") {
                val media = AniListMedia(
                    id = 4,
                    format = "TV",
                    episodes = null,
                    status = "RELEASING",
                    nextAiringEpisode = null
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.FINALE_EXPECTED
                result.isFinale shouldBe false
            }
        }

        `when`("上記いずれの条件も満たさない場合") {
            then("unknown を返す") {
                val media = AniListMedia(
                    id = 5,
                    format = "TV",
                    episodes = 12,
                    status = "RELEASING",
                    nextAiringEpisode = NextAiringEpisode(episode = 10, airingAt = 1678886400) // 次回が現在のエピソード以下
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.UNKNOWN
                result.isFinale shouldBe false
            }
        }

        `when`("format が TV ではない場合") {
            then("unknown を返す") {
                val media = AniListMedia(
                    id = 6,
                    format = "MOVIE",
                    episodes = 1,
                    status = "FINISHED",
                    nextAiringEpisode = null
                )
                coEvery { aniListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(1, media.id)
                result.state shouldBe FinaleState.UNKNOWN
                result.isFinale shouldBe false
            }
        }
    }
})
