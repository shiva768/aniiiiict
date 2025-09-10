package com.zelretch.aniiiiict.ui.details

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.testing.HiltComposeTestRule
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * DetailModalの統合テスト。
 * UI操作からViewModel、UseCaseを経由し、Repository（モック）が
 * 正しく呼び出されるかという、コンポーネント間の連携を検証する。
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class DetailModalIntegrationTest {

    @get:Rule
    val testRule = HiltComposeTestRule(this)

    @Inject
    lateinit var bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase

    @Inject
    lateinit var watchEpisodeUseCase: WatchEpisodeUseCase

    @Inject
    lateinit var updateViewStateUseCase: UpdateViewStateUseCase

    @BindValue
    @JvmField
    val annictRepository: AnnictRepository = mockk<AnnictRepository>().apply {
        coEvery { updateWorkViewStatus(any(), any()) } returns true
        coEvery { createRecord(any(), any()) } returns true
    }

    @BindValue
    @JvmField
    val aniListRepository: AniListRepository = mockk<AniListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val myAnimeListRepository: MyAnimeListRepository = mockk<MyAnimeListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val programFilter: ProgramFilter = mockk<ProgramFilter>(relaxed = true)

    @BindValue
    @JvmField
    val customTabsIntentFactory: CustomTabsIntentFactory = mockk<CustomTabsIntentFactory>().apply {
        every { create() } returns mockk(relaxed = true)
    }

    @Test
    fun detailModal_一括視聴確認_確認でRepository呼び出しをcoVerifyできる() {
        // Arrange
        val viewModel = DetailModalViewModel(bulkRecordEpisodesUseCase, watchEpisodeUseCase, updateViewStateUseCase)

        // 2エピソードのProgramWithWork（WATCHING）
        val work = Work(
            id = "work-verify",
            title = "検証アニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val ep1 = Episode(id = "epA", title = "第1話", numberText = "1", number = 1)
        val ep2 = Episode(id = "epB", title = "第2話", numberText = "2", number = 2)
        val p1 = Program(id = "pA", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val p2 = Program(id = "pB", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep2)
        val pw = ProgramWithWork(programs = listOf(p1, p2), firstProgram = p1, work = work)

        // Act
        testRule.composeTestRule.setContent {
            DetailModal(
                programWithWork = pw,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // 確認ダイアログを表示し、確定をクリック
        viewModel.showConfirmDialog(1)
        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert: createRecord が各エピソードで呼ばれる
        coVerify(exactly = 1) { annictRepository.createRecord("epA", "work-verify") }
        coVerify(exactly = 1) { annictRepository.createRecord("epB", "work-verify") }
        // WATCHING の場合は UpdateViewStateUseCase はステータス更新しないため、updateWorkViewStatus は呼ばれない
        coVerify(exactly = 0) { annictRepository.updateWorkViewStatus(any(), any()) }
    }

    @Test
    fun detailModal_ステータス更新からWATCHED_正しい順序でRepositoryが呼ばれる() {
        // Arrange
        val viewModel = DetailModalViewModel(bulkRecordEpisodesUseCase, watchEpisodeUseCase, updateViewStateUseCase)

        val work = Work(
            id = "work-status",
            title = "ステータス更新アニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WANNA_WATCH
        )
        val ep1 = Episode(id = "epStatus", title = "第1話", numberText = "1", number = 1)
        val p1 = Program(id = "pStatus", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val pw = ProgramWithWork(programs = listOf(p1), firstProgram = p1, work = work)

        // Act
        testRule.composeTestRule.setContent {
            DetailModal(
                programWithWork = pw,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // ステータスをWATCHEDに変更
        viewModel.changeStatus(StatusState.WATCHED)

        // Wait for the coroutine to complete
        testRule.composeTestRule.waitForIdle()

        // Assert: ステータス更新が呼ばれる
        coVerify(exactly = 1) { annictRepository.updateWorkViewStatus("work-status", StatusState.WATCHED) }
    }

    @Test
    fun detailModal_単一エピソード視聴_createRecordが呼ばれる() {
        // Arrange
        val viewModel = DetailModalViewModel(bulkRecordEpisodesUseCase, watchEpisodeUseCase, updateViewStateUseCase)

        val work = Work(
            id = "work-single",
            title = "単一エピソードアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val ep1 = Episode(id = "epSingle", title = "第1話", numberText = "1", number = 1)
        val p1 = Program(id = "pSingle", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val pw = ProgramWithWork(programs = listOf(p1), firstProgram = p1, work = work)

        // Act
        testRule.composeTestRule.setContent {
            DetailModal(
                programWithWork = pw,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // 単一エピソードの視聴記録
        viewModel.showConfirmDialog(0)
        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert
        coVerify(exactly = 1) { annictRepository.createRecord("epSingle", "work-single") }
    }

    @Test
    fun detailModal_WANNA_WATCHからWATCHING経由でのエピソード記録_正しい順序でRepository呼び出し() {
        // Arrange
        val viewModel = DetailModalViewModel(bulkRecordEpisodesUseCase, watchEpisodeUseCase, updateViewStateUseCase)

        val work = Work(
            id = "work-flow",
            title = "フローテストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WANNA_WATCH
        )
        val ep1 = Episode(id = "epFlow", title = "第1話", numberText = "1", number = 1)
        val p1 = Program(id = "pFlow", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val pw = ProgramWithWork(programs = listOf(p1), firstProgram = p1, work = work)

        // Act
        testRule.composeTestRule.setContent {
            DetailModal(
                programWithWork = pw,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // エピソード記録（WANNA_WATCH状態から）
        viewModel.showConfirmDialog(0)
        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert: WANNA_WATCHからの場合、先にWATCHINGに更新してからレコード作成される
        coVerifyOrder {
            annictRepository.updateWorkViewStatus("work-flow", StatusState.WATCHING)
            annictRepository.createRecord("epFlow", "work-flow")
        }
    }
}
