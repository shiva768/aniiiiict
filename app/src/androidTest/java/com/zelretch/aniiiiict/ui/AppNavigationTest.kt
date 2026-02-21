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
import com.zelretch.aniiiiict.testing.FakeAnnictRepository
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
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
    val mockAnnictRepository: AnnictRepository = FakeAnnictRepository()

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
    fun navigationDrawer_menuClick_opensDrawer() {
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

    @Test
    fun navigationDrawer_navigateToHistoryAndBack_keepsDrawerOpen() {
        // Wait for the app to fully load and navigate to the track screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.waitForIdle()

        // Open the drawer
        composeTestRule.onNodeWithContentDescription("メニュー").performClick()
        composeTestRule.waitForIdle()

        // Verify drawer is open
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()

        // Navigate to history
        composeTestRule.onNodeWithText("記録履歴").performClick()
        composeTestRule.waitForIdle()

        // Wait for history screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("視聴履歴").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Navigate back to track screen
        composeTestRule.onNodeWithContentDescription("戻る").performClick()
        composeTestRule.waitForIdle()

        // Wait for track screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Wait a moment for drawer restoration
        Thread.sleep(500) // Give drawer time to restore
        composeTestRule.waitForIdle()

        // Assert that drawer is open after returning to track screen
        // This is the core requirement - drawer should be restored to open state
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()
    }

    @Test
    fun navigationDrawer_navigateToSettingsAndBack_keepsDrawerOpen() {
        // Wait for the app to fully load and navigate to the track screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.waitForIdle()

        // Open the drawer
        composeTestRule.onNodeWithContentDescription("メニュー").performClick()
        composeTestRule.waitForIdle()

        // Verify drawer is open
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithText("設定").performClick()
        composeTestRule.waitForIdle()

        // Wait for settings screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("設定画面").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Navigate back to track screen
        composeTestRule.onNodeWithContentDescription("戻る").performClick()
        composeTestRule.waitForIdle()

        // Wait for track screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Wait a moment for drawer restoration
        Thread.sleep(500) // Give drawer time to restore
        composeTestRule.waitForIdle()

        // Assert that drawer is open after returning to track screen
        // This is the core requirement - drawer should be restored to open state
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()
    }

    @Test
    fun navigationDrawer_drawerStatePreservation_verifyDrawerOpensWhenReturning() {
        // This test specifically verifies the core requirement:
        // "戻ったときにナビゲーションドロワーが開いていることを確認"
        // (Confirm that the navigation drawer is open when returning)

        // Wait for app initialization
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.waitForIdle()

        // Step 1: Open the drawer from track screen
        composeTestRule.onNodeWithContentDescription("メニュー").performClick()
        composeTestRule.waitForIdle()

        // Verify drawer is initially open
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()

        // Step 2: Navigate to history while drawer is open
        composeTestRule.onNodeWithText("記録履歴").performClick()
        composeTestRule.waitForIdle()

        // Wait for history screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("視聴履歴").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Step 3: Navigate back to track screen
        composeTestRule.onNodeWithContentDescription("戻る").performClick()
        composeTestRule.waitForIdle()

        // Wait for track screen to be displayed
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Step 4: CRITICAL VERIFICATION - Drawer should be open when returning
        // Wait for drawer restoration with retries
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
                composeTestRule.onNodeWithText("設定").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Final assertion - this is the core requirement being tested
        composeTestRule.onNodeWithText("記録履歴").assertIsDisplayed()
        composeTestRule.onNodeWithText("設定").assertIsDisplayed()
    }
}
