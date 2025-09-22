package com.zelretch.aniiiiict.ui.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
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

    @Inject
    lateinit var judgeFinaleUseCase: JudgeFinaleUseCase

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
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

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
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

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
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

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
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

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

    @Test
    fun detailModal_WANNA_WATCH_一括視聴_複数話_順序は更新から各話createRecord() {
        // Arrange
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

        val work = Work(
            id = "work-bulk-wanna",
            title = "一括視聴WANNA",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WANNA_WATCH
        )
        val ep1 = Episode(id = "ep-b1", title = "第1話", numberText = "1", number = 1)
        val ep2 = Episode(id = "ep-b2", title = "第2話", numberText = "2", number = 2)
        val p1 = Program(id = "p-b1", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val p2 = Program(id = "p-b2", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep2)
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

        // 一括視聴ダイアログを開いて確定
        viewModel.showConfirmDialog(1)
        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert: WANNA_WATCH の場合、先にWATCHINGに更新 → 各話のcreateRecord
        coVerifyOrder {
            annictRepository.updateWorkViewStatus("work-bulk-wanna", StatusState.WATCHING)
            annictRepository.createRecord("ep-b1", "work-bulk-wanna")
            annictRepository.createRecord("ep-b2", "work-bulk-wanna")
        }
    }

    @Test
    fun detailModal_一括視聴_フィナーレ判定_最終話確認ダイアログ表示() {
        // Arrange
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

        // MAL APIの最終話レスポンスをモック
        val malResponse = MyAnimeListResponse(
            id = 123,
            mediaType = "tv",
            numEpisodes = 12,
            status = "finished_airing",
            broadcast = null
        )
        coEvery { myAnimeListRepository.getMedia(123) } returns Result.success(malResponse)

        val work = Work(
            id = "work-finale",
            title = "フィナーレテスト",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            malAnimeId = "123", // MAL IDを設定
            viewerStatusState = StatusState.WATCHING
        )
        val ep11 = Episode(id = "ep-11", title = "第11話", numberText = "11", number = 11)
        val ep12 = Episode(id = "ep-12", title = "第12話", numberText = "12", number = 12) // 最終話
        val p11 = Program(id = "p-11", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep11)
        val p12 = Program(id = "p-12", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep12)
        val pw = ProgramWithWork(programs = listOf(p11, p12), firstProgram = p11, work = work)

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

        // ViewModel が初期化されるまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 3000) {
            viewModel.state.value.workId.isNotEmpty()
        }

        // 一括視聴ダイアログを開いて確定
        viewModel.showConfirmDialog(1) // 最後の2話を選択

        // ダイアログが表示されるまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                testRule.composeTestRule.onNodeWithText("視聴済みにする").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // 一括記録が完了するまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 10000) {
            !viewModel.state.value.isBulkRecording
        }

        // フィナーレ確認ダイアログが表示されるまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                testRule.composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // フィナーレ確認ダイアログが表示されることを確認
        testRule.composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("第12話は最終話です。").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("視聴完了にする").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("後で").assertIsDisplayed()

        // 視聴完了を選択
        testRule.composeTestRule.onNodeWithText("視聴完了にする").performClick()

        // Assert: エピソード記録 + フィナーレ判定 + ステータス更新
        coVerifyOrder {
            // 最初にエピソード記録
            annictRepository.createRecord("ep-11", "work-finale")
            annictRepository.createRecord("ep-12", "work-finale")
            // MALからメディア情報取得（フィナーレ判定のため）
            myAnimeListRepository.getMedia(123)
            // 最終話確認後にWATCHEDに更新
            annictRepository.updateWorkViewStatus("work-finale", StatusState.WATCHED)
        }
    }

    @Test
    fun detailModal_単一エピソード記録_フィナーレ判定_最終話確認ダイアログ表示() {
        // Arrange
        val viewModel =
            DetailModalViewModel(
                bulkRecordEpisodesUseCase,
                watchEpisodeUseCase,
                updateViewStateUseCase,
                judgeFinaleUseCase
            )

        // MAL APIの最終話レスポンスをモック
        val malResponse = MyAnimeListResponse(
            id = 456,
            mediaType = "tv",
            numEpisodes = 12,
            status = "finished_airing",
            broadcast = null
        )
        coEvery { myAnimeListRepository.getMedia(456) } returns Result.success(malResponse)

        val work = Work(
            id = "work-single-finale",
            title = "単一エピソードフィナーレテスト",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            malAnimeId = "456", // MAL IDを設定
            viewerStatusState = StatusState.WATCHING
        )
        val ep12 = Episode(id = "ep-single-12", title = "第12話", numberText = "12", number = 12) // 最終話
        val p12 = Program(id = "p-single-12", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep12)
        val pw = ProgramWithWork(programs = listOf(p12), firstProgram = p12, work = work)

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

        // ViewModel が初期化されるまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 3000) {
            viewModel.state.value.workId.isNotEmpty()
        }

        // 単一エピソードの記録ボタンをクリック
        testRule.composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                testRule.composeTestRule.onNodeWithText("記録する").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        testRule.composeTestRule.onNodeWithText("記録する").performClick()

        // 記録処理が完了するまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 10000) {
            viewModel.state.value.showSingleEpisodeFinaleConfirmation
        }

        // フィナーレ確認ダイアログが表示されるまで待機
        testRule.composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                testRule.composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // フィナーレ確認ダイアログが表示されることを確認
        testRule.composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("第12話は最終話です。").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("視聴完了にする").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("後で").assertIsDisplayed()

        // 視聴完了を選択
        testRule.composeTestRule.onNodeWithText("視聴完了にする").performClick()

        // Assert: エピソード記録 + フィナーレ判定 + ステータス更新
        coVerifyOrder {
            // 最初にエピソード記録
            annictRepository.createRecord("ep-single-12", "work-single-finale")
            // MALからメディア情報取得（フィナーレ判定のため）
            myAnimeListRepository.getMedia(456)
            // 最終話確認後にWATCHEDに更新
            annictRepository.updateWorkViewStatus("work-single-finale", StatusState.WATCHED)
        }
    }
}
