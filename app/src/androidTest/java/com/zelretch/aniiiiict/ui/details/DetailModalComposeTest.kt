package com.zelretch.aniiiiict.ui.details

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.common.LocalTestMode
import com.zelretch.aniiiiict.util.DisableAnimationsRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * DetailModal のCompose UIテスト
 * ステータス変更ドロップダウン等、重要要素の破損検知を目的とする
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DetailModalComposeTest {

    private val composeTestRule = createComposeRule()
    private val disableAnimationsRule = DisableAnimationsRule()

    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(disableAnimationsRule)
        .around(composeTestRule)

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var annictRepository: AnnictRepository
    private lateinit var viewModel: DetailModalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        annictRepository = mockk<AnnictRepository>()
        coEvery { annictRepository.updateWorkViewStatus(any(), any()) } returns true
        coEvery { annictRepository.createRecord(any(), any()) } returns true
        val update = UpdateViewStateUseCase(annictRepository)
        val watch = WatchEpisodeUseCase(annictRepository, update)
        val bulk = BulkRecordEpisodesUseCase(watch)
        viewModel = DetailModalViewModel(bulk, watch, update)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleProgramWithWork(
        status: StatusState = StatusState.WATCHING,
        episodeCount: Int = 1
    ): ProgramWithWork {
        val work = Work(
            id = "work-1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = status
        )
        val episodes = (1..episodeCount).map {
            Episode(
                id = "ep$it",
                title = "第${it}話",
                numberText = "$it",
                number = it
            )
        }
        val programs = episodes.map {
            Program(
                id = "prog${it.number}",
                startedAt = LocalDateTime.now(),
                channel = Channel(name = "チャンネル"),
                episode = it
            )
        }
        return ProgramWithWork(
            programs = programs,
            firstProgram = programs.first(),
            work = work
        )
    }

    @Test
    fun detailModal_基本要素_タイトルと閉じるボタンが表示される() {
        val programWithWork = sampleProgramWithWork()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                DetailModal(
                    programWithWork = programWithWork,
                    isLoading = false,
                    onDismiss = {},
                    viewModel = viewModel,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("未視聴エピソード (1件)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("閉じる").assertIsDisplayed()
    }

    @Test
    fun detailModal_ステータスドロップダウン_展開して選択できる() {
        val programWithWork = sampleProgramWithWork(StatusState.WATCHING)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                DetailModal(
                    programWithWork = programWithWork,
                    isLoading = false,
                    onDismiss = {},
                    viewModel = viewModel,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("WATCHING").performClick()
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()
        composeTestRule.onNodeWithText("WATCHED").performClick()

        coVerify { annictRepository.updateWorkViewStatus("work-1", StatusState.WATCHED) }
    }

    @Test
    fun detailModal_バルク記録_正常に記録できる() {
        val programWithWork = sampleProgramWithWork(episodeCount = 3)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                DetailModal(
                    programWithWork = programWithWork,
                    isLoading = false,
                    onDismiss = {},
                    viewModel = viewModel,
                    onRefresh = {}
                )
            }
        }

        // 2番目のエピソードをクリックしてダイアログ表示
        composeTestRule.onNodeWithText("第2話").performClick()
        composeTestRule.onNodeWithText("「第2話」までを一括で視聴済みにしますか？").assertIsDisplayed()

        // ダイアログのOKボタンをクリック
        composeTestRule.onNodeWithText("視聴済みにする").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000) // アニメーションや遅延処理を待つ

        // Repositoryの呼び出しを検証
        coVerify { annictRepository.createRecord("ep1", "work-1") }
        coVerify { annictRepository.createRecord("ep2", "work-1") }
        coVerify(exactly = 0) { annictRepository.createRecord("ep3", "work-1") }
    }

    @Test
    fun detailModal_バルク記録_記録に失敗した場合() {
        // 2番目の記録で失敗させる
        coEvery { annictRepository.createRecord("ep1", any()) } returns true
        coEvery { annictRepository.createRecord("ep2", any()) } throws RuntimeException("API Error")

        val programWithWork = sampleProgramWithWork(episodeCount = 3)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                DetailModal(
                    programWithWork = programWithWork,
                    isLoading = false,
                    onDismiss = {},
                    viewModel = viewModel,
                    onRefresh = {}
                )
            }
        }

        // 2番目のエピソードをクリック
        composeTestRule.onNodeWithText("第2話").performClick()

        // 視聴済みにする
        composeTestRule.onNodeWithText("視聴済みにする").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        // ダイアログが閉じられていることを確認
        composeTestRule.onNodeWithText("「第2話」までを一括で視聴済みにしますか？").assertDoesNotExist()

        // 失敗してもクラッシュしないこと、最初のエピソードは記録されていることを確認
        coVerify { annictRepository.createRecord("ep1", "work-1") }
        coVerify { annictRepository.createRecord("ep2", "work-1") }
    }
}
