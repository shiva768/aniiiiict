package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class JudgeFinaleUseCaseTest : BehaviorSpec({

    val myAnimeListRepository = mockk<MyAnimeListRepository>()
    val judgeFinaleUseCase = JudgeFinaleUseCase(myAnimeListRepository)

    given("最終話判定ロジック") {
        `when`("status == currently_airing の場合") {
            then("not_finale を返す") {
                val media = MyAnimeListResponse(
                    id = 1,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "currently_airing",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.NOT_FINALE
                result.isFinale shouldBe false
            }
        }

        `when`("numEpisodes が数値 かつ currentEp >= numEpisodes の場合") {
            then("finale_confirmed を返す") {
                val media = MyAnimeListResponse(
                    id = 2,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "currently_airing",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(12, media.id)
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("status == finished_airing の場合") {
            then("finale_confirmed を返す") {
                val media = MyAnimeListResponse(
                    id = 3,
                    mediaType = "tv",
                    numEpisodes = null,
                    status = "finished_airing",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.FINALE_CONFIRMED
                result.isFinale shouldBe true
            }
        }

        `when`("currently_airing かつ numEpisodes == null の場合") {
            then("unknown を返す") {
                val media = MyAnimeListResponse(
                    id = 4,
                    mediaType = "tv",
                    numEpisodes = null,
                    status = "currently_airing",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.UNKNOWN
                result.isFinale shouldBe false
            }
        }

        `when`("上記いずれの条件も満たさない場合") {
            then("unknown を返す") {
                val media = MyAnimeListResponse(
                    id = 5,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "not_yet_aired",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(10, media.id)
                result.state shouldBe FinaleState.UNKNOWN
                result.isFinale shouldBe false
            }
        }

        `when`("mediaType が tv ではない場合") {
            then("unknown を返す") {
                val media = MyAnimeListResponse(
                    id = 6,
                    mediaType = "movie",
                    numEpisodes = 1,
                    status = "finished_airing",
                    broadcast = null
                )
                coEvery { myAnimeListRepository.getMedia(media.id) } returns Result.success(media)
                val result = judgeFinaleUseCase(1, media.id)
                result.state shouldBe FinaleState.UNKNOWN
                result.isFinale shouldBe false
            }
        }
    }
})
