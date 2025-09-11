package com.zelretch.aniiiiict.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zelretch.aniiiiict.MainActivity
import com.zelretch.aniiiiict.data.auth.TokenManager
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
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val mockAnnictRepository: AnnictRepository = mockk(relaxed = true) {
        coEvery { getRawProgramsData() } returns flowOf(emptyList())
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

    @BindValue
    @JvmField
    val mockTokenManager: TokenManager = mockk(relaxed = true) {
        coEvery { hasValidToken() } returns true
    }

    @Test
    fun navigationDrawer_menuClick_opensDrawer() {
        // Wait for the app to navigate to the track screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("番組一覧").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Click the menu button
        composeTestRule.onNodeWithContentDescription("メニュー").performClick()

        // Assert that the drawer is open and the items are displayed
        composeTestRule.onNodeWithText("作品一覧").assertIsDisplayed()
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()
    }
}
