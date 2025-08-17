package com.zelretch.aniiiiiict.ui.mypage

import app.cash.turbine.test
import com.zelretch.aniiiiiict.domain.usecase.GetMyActivitiesUseCase
import com.zelretch.aniiiiiict.domain.usecase.MyActivity
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.data.model.Episode
import com.annict.type.StatusState
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

@ExperimentalCoroutinesApi
class MyPageViewModelTest {

    private lateinit var viewModel: MyPageViewModel
    private val getMyActivitiesUseCase: GetMyActivitiesUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadActivities success - updates state with grouped activities`() = runTest {
        // Given
        val activities = listOf(
            MyActivity(
                record = Record("1", null, null, ZonedDateTime.parse("2023-01-15T10:00:00Z"), Episode("e1", null, "", "", false), Work("w1", "Work 1", viewerStatusState = StatusState.WATCHING)),
                genres = listOf("Action"),
                studios = listOf("Studio A")
            ),
            MyActivity(
                record = Record("2", null, null, ZonedDateTime.parse("2023-02-20T10:00:00Z"), Episode("e2", null, "", "", false), Work("w2", "Work 2", viewerStatusState = StatusState.WATCHING)),
                genres = listOf("Comedy"),
                studios = listOf("Studio B")
            )
        )
        coEvery { getMyActivitiesUseCase() } returns flowOf(activities)

        // When
        viewModel = MyPageViewModel(getMyActivitiesUseCase)

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            initialState.isLoading shouldBe true

            val successState = awaitItem()
            successState.isLoading shouldBe false
            successState.error should beNull()
            successState.activitiesByMonth shouldHaveSize 1 // 1 year (2023)
            successState.activitiesByMonth[2023]!! shouldHaveSize 2 // 2 months (Jan, Feb)
            successState.activitiesByMonth[2023]!![1]!! shouldHaveSize 1
            successState.activitiesByMonth[2023]!![2]!! shouldHaveSize 1
            cancelAndConsumeRemainingEvents()
        }
    }
}
