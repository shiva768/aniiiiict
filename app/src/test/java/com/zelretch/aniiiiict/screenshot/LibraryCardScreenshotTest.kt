package com.zelretch.aniiiiict.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.github.takahirom.roborazzi.captureRoboImage
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.ui.library.LibraryEntryCard
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * LibraryEntryCard（§2 ステータス色レール＋進捗＋「見た」クイック記録）のビジュアル回帰テスト。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LibraryCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun entry(status: StatusState, withNext: Boolean): LibraryEntry = LibraryEntry(
        id = "entry1",
        work = Work(
            id = "work1",
            title = "サンプルアニメ タイトル",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            viewerStatusState = status
        ),
        nextEpisode = if (withNext) {
            Episode(id = "ep9", title = "学園の子供たち", numberText = "第9話", number = 9)
        } else {
            null
        },
        statusState = status
    )

    @Composable
    private fun CardHost(dark: Boolean, content: @Composable () -> Unit) {
        AniiiiictTheme(darkTheme = dark, dynamicColor = false) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.width(420.dp)) { content() }
            }
        }
    }

    private fun renderEntry(dark: Boolean, status: StatusState, withNext: Boolean) {
        composeRule.setContent {
            CardHost(dark = dark) {
                LibraryEntryCard(
                    entry = entry(status, withNext),
                    isRecording = false,
                    onClick = {},
                    onRecordNextEpisode = {}
                )
            }
        }
    }

    @Test
    fun libraryCard_視聴中_light() {
        renderEntry(dark = false, status = StatusState.WATCHING, withNext = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/LibraryCard_watching_light.png")
    }

    @Test
    fun libraryCard_視聴中_dark() {
        renderEntry(dark = true, status = StatusState.WATCHING, withNext = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/LibraryCard_watching_dark.png")
    }

    @Test
    fun libraryCard_視聴済み_light() {
        renderEntry(dark = false, status = StatusState.WATCHED, withNext = false)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/LibraryCard_watched_light.png")
    }
}
