package com.zelretch.aniiiiict.ui.library

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.testing.FakeAnnictRepository
import com.zelretch.aniiiiict.testing.HiltComposeTestRule
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * WatchingEpisodeModalの統合テスト。
 * UI操作からViewModel、UseCaseを経由し、Repository（モック）が
 * 正しく呼び出されるかという、コンポーネント間の連携を検証する。
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class WatchingEpisodeModalIntegrationTest {

    @get:Rule
    val testRule = HiltComposeTestRule(this)

    @Inject
    lateinit var watchEpisodeUseCase: WatchEpisodeUseCase

    @Inject
    lateinit var updateViewStateUseCase: UpdateViewStateUseCase

    @Inject
    lateinit var judgeFinaleUseCase: JudgeFinaleUseCase

    @Inject
    lateinit var errorMapper: ErrorMapper

    private val fakeAnnictRepository = FakeAnnictRepository()

    @BindValue
    @JvmField
    val annictRepository: AnnictRepository = fakeAnnictRepository

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
    fun watchingEpisodeModal_視聴済みボタンクリック_Repository呼び出しをcoVerifyできる() {
        // Arrange
        val viewModel =
            WatchingEpisodeModalViewModel(watchEpisodeUseCase, updateViewStateUseCase, judgeFinaleUseCase, errorMapper)

        val work = Work(
            id = "work-integration",
            title = "統合テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING,
            noEpisodes = false
        )
        val episode = Episode(id = "ep-test", title = "第1話", numberText = "1", number = 1)
        val entry =
            LibraryEntry(id = "lib-entry-int1", work = work, statusState = StatusState.WATCHING, nextEpisode = episode)

        // Act
        testRule.composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        testRule.composeTestRule.onNodeWithText("視聴済みにする").performClick()

        testRule.composeTestRule.waitForIdle()

        // Assert - AnnictRepository.createRecordが呼ばれたことを確認
        assertTrue(
            fakeAnnictRepository.createRecordCalls.any { it.first == "ep-test" }
        )
    }

    @Test
    fun watchingEpisodeModal_ステータス変更_Repository呼び出しをcoVerifyできる() {
        // Arrange
        val viewModel =
            WatchingEpisodeModalViewModel(watchEpisodeUseCase, updateViewStateUseCase, judgeFinaleUseCase, errorMapper)

        val work = Work(
            id = "work-status",
            title = "ステータステストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING,
            noEpisodes = false
        )
        val episode = Episode(id = "ep-status", title = "第1話", numberText = "1", number = 1)
        val entry =
            LibraryEntry(id = "lib-entry-int2", work = work, statusState = StatusState.WATCHING, nextEpisode = episode)

        // Act
        testRule.composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // ステータスドロップダウンを開いて選択
        testRule.composeTestRule.onNodeWithText("WATCHING").performClick()
        testRule.composeTestRule.waitForIdle()
        testRule.composeTestRule.onNodeWithText("WATCHED").performClick()
        testRule.composeTestRule.waitForIdle()

        // Assert - AnnictRepository.updateWorkViewStatusが呼ばれたことを確認
        assertTrue(
            fakeAnnictRepository.updateWorkViewStatusCalls.contains("work-status" to StatusState.WATCHED)
        )
    }

    @Test
    fun watchingEpisodeModal_エピソードなし_視聴済みボタンが表示されない() {
        // Arrange
        val viewModel =
            WatchingEpisodeModalViewModel(watchEpisodeUseCase, updateViewStateUseCase, judgeFinaleUseCase, errorMapper)

        val work = Work(
            id = "work-no-ep",
            title = "エピソードなしアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING,
            noEpisodes = false
        )
        val entry =
            LibraryEntry(id = "lib-entry-int3", work = work, statusState = StatusState.WATCHING, nextEpisode = null)

        // Act
        testRule.composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        testRule.composeTestRule.onNodeWithText("次のエピソード情報がありません").assertExists()
        testRule.composeTestRule.onNodeWithText("視聴済みにする").assertDoesNotExist()
    }

    @Test
    fun watchingEpisodeModal_noEpisodesがtrue_エピソード記録UIが非表示でステータス変更のみ可能() {
        // Arrange
        val viewModel =
            WatchingEpisodeModalViewModel(watchEpisodeUseCase, updateViewStateUseCase, judgeFinaleUseCase, errorMapper)

        val work = Work(
            id = "work-no-episodes",
            title = "エピソード情報なし作品",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING,
            noEpisodes = true
        )
        val entry =
            LibraryEntry(
                id = "lib-entry-no-episodes",
                work = work,
                statusState = StatusState.WATCHING,
                nextEpisode = null
            )

        // Act
        testRule.composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        testRule.composeTestRule.onNodeWithText("この作品にはエピソード情報がありません。ステータスの変更のみ可能です。").assertExists()
        testRule.composeTestRule.onNodeWithText("視聴済みにする").assertDoesNotExist()
        testRule.composeTestRule.onNodeWithText("次のエピソード").assertDoesNotExist()

        // Verify status change is still possible
        testRule.composeTestRule.onNodeWithText("WATCHING").assertExists()
    }
}
