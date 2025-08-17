package com.zelretch.aniiiiiict.ui.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.domain.usecase.MyActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class MyPageScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel: MyPageViewModel = mockk(relaxed = true)
    private val mockOnNavigateToHistory: () -> Unit = mockk(relaxed = true)

    @Test
    fun myPageScreen_displaysDataCorrectly() {
        // Given
        val activity = MyActivity(
            record = Record("1", null, null, ZonedDateTime.parse("2023-01-15T10:00:00Z"), Episode("e1", null, "", "", false), Work("w1", "Work 1", viewerStatusState = StatusState.WATCHING)),
            genres = listOf("Action"),
            studios = listOf("Studio A")
        )
        val state = MyPageUiState(
            isLoading = false,
            activitiesByMonth = mapOf(2023 to mapOf(1 to listOf(activity))),
            error = null
        )
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // When
        composeTestRule.setContent {
            MyPageScreen(
                viewModel = mockViewModel,
                onNavigateToHistory = mockOnNavigateToHistory
            )
        }

        // Then
        composeTestRule.onNodeWithText("Year: 2023").assertIsDisplayed()
        composeTestRule.onNodeWithText("  Month: 1 (1 records)").assertIsDisplayed()
    }

    @Test
    fun myPageScreen_navigationButton_callsLambda() {
        // Given
        val state = MyPageUiState(isLoading = false, error = null)
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // When
        composeTestRule.setContent {
            MyPageScreen(
                viewModel = mockViewModel,
                onNavigateToHistory = mockOnNavigateToHistory
            )
        }
        composeTestRule.onNodeWithText("Go to Full History").performClick()

        // Then
        verify { mockOnNavigateToHistory() }
    }
}
