package com.zelretch.aniiiiict.ui.common

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zelretch.aniiiiict.ui.common.components.ANNICT_OUTAGE_BANNER_TAG
import com.zelretch.aniiiiict.ui.common.components.AnnictOutageBanner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnictOutageBannerUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun annictOutageBanner_障害メッセージが表示される() {
        composeTestRule.setContent {
            AnnictOutageBanner()
        }

        composeTestRule.onNodeWithTag(ANNICT_OUTAGE_BANNER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Annictで障害が発生している可能性があります", substring = true).assertIsDisplayed()
    }
}
