package com.zelretch.aniiiiict.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zelretch.aniiiiict.MainActivity
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(AppModule::class)
class アプリナビゲーションテスト {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val mockAnnictRepository: AnnictRepository = mockk(relaxed = true) {
        coEvery { getRawProgramsData() } returns flowOf(emptyList())
        coEvery { isAuthenticated() } returns true
    }

    @BindValue
    @JvmField
    val mockMyAnimeListRepository: MyAnimeListRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockProgramFilter: ProgramFilter = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockCustomTabsIntentFactory: CustomTabsIntentFactory = mockk(relaxed = true)

    @Test
    fun ナビゲーションドロワーでメニューをクリックするとドロワーが開く() {
        // Wait for the app to fully load and navigate to the track screen
        // Look for the menu button which indicates the track screen is loaded
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Additional wait to ensure the screen is fully rendered
        composeTestRule.waitForIdle()

        // Click the menu button
        composeTestRule.onNodeWithContentDescription("メニュー").performClick()

        // Wait for the drawer to open
        composeTestRule.waitForIdle()

        // Assert that the drawer is open and the items are displayed
        // We need to be more specific since "視聴記録" appears both in the app bar and drawer
        // Let's check for the presence of all drawer items together to confirm the drawer is open
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()
    }
}
