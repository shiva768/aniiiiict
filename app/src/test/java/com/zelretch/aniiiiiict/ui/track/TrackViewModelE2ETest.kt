package com.zelretch.aniiiiiict.ui.track

import app.cash.turbine.test
import com.annict.ViewerProgramsQuery
import com.annict.type.Media
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.util.TestLogger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * E2Eスタイルのテスト
 * ViewModelからUseCaseまでの実装を使用し、Repositoryのみをモック化する
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelE2ETest : BehaviorSpec({
    val dispatcher = UnconfinedTestDispatcher()
    val filterStateFlow = MutableStateFlow(FilterState())
    val filterPreferences = mockk<FilterPreferences> {
        every { filterState } returns filterStateFlow
        coEvery { updateFilterState(any()) } returns Unit
    }

    // リポジトリをモック化
    val annictRepository = mockk<AnnictRepository>()
    val aniListRepository = mockk<AniListRepository>()

    // 実際のUseCaseを使用
    val loadProgramsUseCase = LoadProgramsUseCase(annictRepository)
    val updateViewStateUseCase = UpdateViewStateUseCase(annictRepository, TestLogger())
    val watchEpisodeUseCase = WatchEpisodeUseCase(annictRepository, updateViewStateUseCase)
    val programFilter = ProgramFilter()
    val filterProgramsUseCase = FilterProgramsUseCase(programFilter)
    val judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()

    lateinit var viewModel: TrackViewModel
    lateinit var testScope: TestScope

    beforeTest {
        Dispatchers.setMain(dispatcher)
        testScope = TestScope(dispatcher)

        // デフォルトのモック動作を設定
        coEvery { annictRepository.getRawProgramsData() } returns flowOf(emptyList())
        coEvery { annictRepository.createRecord(any(), any()) } returns true
        coEvery { annictRepository.updateWorkViewStatus(any(), any()) } returns true
        coEvery { aniListRepository.getMedia(any()) } returns Result.success(
            AniListMedia(
                id = 1,
                episodes = 12,
                format = "TV",
                status = "RELEASING",
                nextAiringEpisode = NextAiringEpisode(
                    episode = 2,
                    airingAt = 0
                )
            )
        )

        viewModel = TrackViewModel(
            loadProgramsUseCase,
            watchEpisodeUseCase,
            filterProgramsUseCase,
            filterPreferences,
            judgeFinaleUseCase,
            TestLogger()
        )
        viewModel.externalScope = testScope // テスト用スコープをセット
        testScope.testScheduler.advanceUntilIdle() // ViewModelのinitコルーチンを確実に進める
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("プログラム一覧ロード（E2Eスタイル）") {
        `when`("正常にロードできる場合") {
            then("UIStateにプログラムがセットされる") {
                runTest {
                    // モックリポジトリの動作を設定
                    val mockPrograms = createMockPrograms()
                    coEvery { annictRepository.getRawProgramsData() } returns flowOf(mockPrograms)

                    // フィルター変更をトリガーにしてロード処理を実行
                    filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("TV"))
                    testScope.testScheduler.advanceUntilIdle()

                    // テストスケジューラを進めて、すべての非同期処理が完了するのを待つ
                    testScope.testScheduler.advanceTimeBy(2000) // ローディング処理が完了するのを待つ
                    testScope.testScheduler.runCurrent()

                    // 最終的なUIStateを検証
                    viewModel.uiState.value.isLoading shouldBe false
                    viewModel.uiState.value.programs.isEmpty() shouldBe false
                    viewModel.uiState.value.error shouldBe null

                    // リポジトリが呼ばれたことを検証
                    coVerify { annictRepository.getRawProgramsData() }
                }
            }
        }

        `when`("例外が発生する場合") {
            then("UIStateにエラーがセットされる") {
                runTest {
                    // モックリポジトリにエラーを発生させる
                    coEvery { annictRepository.getRawProgramsData() } throws
                        RuntimeException("テストエラー")

                    // フィルター変更をトリガーにしてロード処理を実行
                    filterStateFlow.value =
                        filterStateFlow.value.copy(selectedMedia = setOf("dummy-error"))
                    testScope.testScheduler.advanceUntilIdle()

                    // テストスケジューラを進めて、すべての非同期処理が完了するのを待つ
                    testScope.testScheduler.advanceTimeBy(2000) // エラー処理が完了するのを待つ
                    testScope.testScheduler.runCurrent()

                    // 最終的なUIStateを検証
                    viewModel.uiState.value.isLoading shouldBe false
                    viewModel.uiState.value.error shouldBe "テストエラー"

                    // リポジトリが呼ばれたことを検証
                    coVerify { annictRepository.getRawProgramsData() }
                }
            }
        }
    }

    given("エピソード視聴記録（E2Eスタイル）") {
        `when`("正常に記録できる場合") {
            then("記録が成功し、UIStateが更新される") {
                runTest {
                    // モックリポジトリの動作を設定
                    coEvery { annictRepository.createRecord(any(), any()) } returns true
                    coEvery { annictRepository.getRawProgramsData() } returns
                        flowOf(createMockPrograms())

                    // エピソード視聴を記録
                    viewModel.recordEpisode("ep-id", "work-id", StatusState.WATCHING)

                    // 非同期処理が完了するのを待つ（ただし2000msのdelayの前に検証する）
                    testScope.testScheduler.advanceTimeBy(100)
                    testScope.testScheduler.runCurrent()

                    // UIStateを検証
                    viewModel.uiState.value.recordingSuccess shouldBe "ep-id"
                    viewModel.uiState.value.error shouldBe null

                    // リポジトリが呼ばれたことを検証
                    coVerify { annictRepository.createRecord("ep-id", "work-id") }
                }
            }
        }

        `when`("記録に失敗する場合") {
            then("UIStateにエラーがセットされる") {
                runTest {
                    // モックリポジトリにエラーを発生させる
                    coEvery { annictRepository.createRecord(any(), any()) } throws
                        RuntimeException("記録エラー")

                    // エピソード視聴を記録
                    viewModel.recordEpisode("ep-id", "work-id", StatusState.WATCHING)
                    testScope.testScheduler.advanceUntilIdle()

                    // UIStateを検証
                    viewModel.uiState.test {
                        val state = awaitItem()
                        state.error shouldNotBe null
                        state.recordingSuccess shouldBe null

                        // リポジトリが呼ばれたことを検証
                        coVerify { annictRepository.createRecord("ep-id", "work-id") }
                    }
                }
            }
        }

        `when`("最終話を視聴した場合") {
            then("最終話確認ダイアログが表示される") {
                runTest {
                    // モックリポジトリの動作を設定
                    val mockPrograms = createMockPrograms()
                    coEvery { annictRepository.getRawProgramsData() } returns flowOf(mockPrograms)
                    coEvery { annictRepository.createRecord(any(), any()) } returns true

                    // 最終話判定のためのJudgeFinaleUseCaseをモック
                    coEvery {
                        judgeFinaleUseCase.invoke(any(), any())
                    } returns JudgeFinaleResult(
                        state = FinaleState.FINALE_CONFIRMED,
                        isFinale = true
                    )

                    // エピソード視聴を記録
                    viewModel.recordEpisode("ep-id", "123", StatusState.WATCHING)

                    // 非同期処理が完了するのを待つ
                    testScope.testScheduler.advanceTimeBy(3000) // loadingProgramsの処理時間(2000ms)より長く待つ
                    testScope.testScheduler.runCurrent()

                    // デバッグ用にuiStateの現在値をログ出力
                    println("[DEBUG_LOG] Current uiState: ${viewModel.uiState.value}")

                    // 最終話確認ダイアログの状態を検証
                    viewModel.uiState.value.showFinaleConfirmationForWorkId shouldBe "123"
                    viewModel.uiState.value.showFinaleConfirmationForEpisodeNumber shouldBe 1

                    // リポジトリが呼ばれたことを検証
                    coVerify { annictRepository.createRecord("ep-id", "123") }
                    // JudgeFinaleUseCaseが呼ばれたことを検証
                    coVerify { judgeFinaleUseCase.invoke(1, 123) }
                }
            }

            then("確認ダイアログを閉じることができる") {
                runTest {
                    // モックリポジトリの動作を設定
                    val mockPrograms = createMockPrograms()
                    coEvery { annictRepository.getRawProgramsData() } returns flowOf(mockPrograms)
                    coEvery { annictRepository.createRecord(any(), any()) } returns true

                    // 最終話判定のためのJudgeFinaleUseCaseをモック
                    coEvery {
                        judgeFinaleUseCase.invoke(any(), any())
                    } returns JudgeFinaleResult(
                        state = FinaleState.FINALE_CONFIRMED,
                        isFinale = true
                    )

                    // エピソード視聴を記録して確認ダイアログを表示
                    viewModel.recordEpisode("ep-id", "123", StatusState.WATCHING)
                    testScope.testScheduler.advanceTimeBy(3000)
                    testScope.testScheduler.runCurrent()

                    // 確認ダイアログを閉じる
                    viewModel.dismissFinaleConfirmation()

                    // ダイアログが閉じられたことを検証
                    viewModel.uiState.value.showFinaleConfirmationForWorkId shouldBe null
                    viewModel.uiState.value.showFinaleConfirmationForEpisodeNumber shouldBe null
                }
            }

            then("確認ダイアログで「視聴済み」を選択できる") {
                runTest {
                    // モックリポジトリの動作を設定
                    val mockPrograms = createMockPrograms()
                    coEvery { annictRepository.getRawProgramsData() } returns flowOf(mockPrograms)
                    coEvery { annictRepository.createRecord(any(), any()) } returns true
                    coEvery { annictRepository.updateWorkViewStatus(any(), any()) } returns true

                    // 最終話判定のためのJudgeFinaleUseCaseをモック
                    coEvery {
                        judgeFinaleUseCase.invoke(any(), any())
                    } returns JudgeFinaleResult(
                        state = FinaleState.FINALE_CONFIRMED,
                        isFinale = true
                    )

                    // エピソード視聴を記録して確認ダイアログを表示
                    viewModel.recordEpisode("ep-id", "123", StatusState.WATCHING)
                    testScope.testScheduler.advanceTimeBy(3000)
                    testScope.testScheduler.runCurrent()

                    // 視聴済みステータスに更新
                    viewModel.confirmWatchedStatus()
                    testScope.testScheduler.advanceTimeBy(1000)
                    testScope.testScheduler.runCurrent()

                    // ダイアログが閉じられたことを検証
                    viewModel.uiState.value.showFinaleConfirmationForWorkId shouldBe null
                    viewModel.uiState.value.showFinaleConfirmationForEpisodeNumber shouldBe null

                    // watchEpisodeUseCaseが正しいパラメータで呼ばれたことを検証
                    coVerify { watchEpisodeUseCase("", "123", StatusState.WATCHED, true) }
                }
            }
        }
    }
})

// テスト用のモックプログラムデータを作成
private fun createMockPrograms(): List<ViewerProgramsQuery.Node?> = listOf(
    mockk<ViewerProgramsQuery.Node> {
        every { id } returns "prog-id-1"
        every { startedAt } returns "2025-01-01T12:00:00Z"
        every { channel } returns mockk {
            every { name } returns "テレビ東京"
        }
        every { episode } returns mockk {
            every { id } returns "ep-id"
            every { number } returns 1
            every { numberText } returns "#1"
            every { title } returns "エピソードタイトル"
        }
        every { work } returns mockk {
            every { id } returns "123"
            every { title } returns "作品タイトル"
            every { seasonName } returns SeasonName.WINTER
            every { seasonYear } returns 2025
            every { media } returns Media.TV
            every { viewerStatusState } returns StatusState.WATCHING
            every { image } returns mockk {
                every { recommendedImageUrl } returns "https://example.com/image.jpg"
                every { facebookOgImageUrl } returns "https://example.com/og_image.jpg"
            }
        }
    }
)
