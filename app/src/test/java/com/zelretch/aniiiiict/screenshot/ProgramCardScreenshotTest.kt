package com.zelretch.aniiiiict.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.github.takahirom.roborazzi.captureRoboImage
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiict.ui.track.TrackUiState
import com.zelretch.aniiiiict.ui.track.components.ProgramCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/**
 * ProgramCard（§1 デュアルCTA＋インライン一括記録）のビジュアル回帰テスト。
 * Roborazzi でホスト側（エミュレータ不要）にスクリーンショットを撮り、参照画像と差分比較する。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProgramCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleProgram(): ProgramWithWork {
        val work = Work(
            id = "1",
            title = "サンプルアニメ タイトル",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val programs = (0..2).map { i ->
            Program(
                id = "prog$i",
                startedAt = LocalDateTime.of(2024, 4, 1 + i, 23, 0),
                channel = Channel(name = "テレビ東京"),
                episode = Episode(id = "ep$i", title = "エピソード${i + 1}", numberText = "${i + 1}", number = i + 1)
            )
        }
        return ProgramWithWork(programs = programs, work = work)
    }

    @Composable
    private fun CardHost(dark: Boolean, content: @Composable () -> Unit) {
        AniiiiictTheme(darkTheme = dark, dynamicColor = false) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.width(420.dp)) { content() }
            }
        }
    }

    private fun renderCard(dark: Boolean) {
        composeRule.setContent {
            CardHost(dark = dark) {
                ProgramCard(
                    programWithWork = sampleProgram(),
                    onRecordEpisode = { _, _, _ -> },
                    onBulkRecordUpTo = {},
                    onShowAnimeDetail = {},
                    uiState = TrackUiState()
                )
            }
        }
    }

    @Test
    fun programCard_通常_light() {
        renderCard(dark = false)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/ProgramCard_normal_light.png")
    }

    @Test
    fun programCard_通常_dark() {
        renderCard(dark = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/ProgramCard_normal_dark.png")
    }

    @Test
    fun programCard_まとめて展開_light() {
        renderCard(dark = false)
        composeRule.onNodeWithText("まとめて").performClick()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/ProgramCard_expanded_light.png")
    }
}
