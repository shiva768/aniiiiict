package com.zelretch.aniiiiiict.ui.mypage

import app.cash.turbine.test
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class MyPageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val annictRepository: AnnictRepository = mockk()
    private val aniListRepository: AniListRepository = mockk()

    private lateinit var viewModel: MyPageViewModel

    @Test
    fun `loadData should update state with contribution data`() = runTest {
        // Given
        val records = listOf(
            Record(
                id = "1",
                createdAt = ZonedDateTime.parse("2023-01-01T10:00:00Z"),
                work = Work("w1", "Work 1", "123", null, null, null, com.annict.type.StatusState.WATCHING, null, null),
                comment = null,
                rating = null,
                episode = mockk()
            ),
            Record(
                id = "2",
                createdAt = ZonedDateTime.parse("2023-01-01T11:00:00Z"),
                work = Work("w1", "Work 1", "123", null, null, null, com.annict.type.StatusState.WATCHING, null, null),
                comment = null,
                rating = null,
                episode = mockk()
            )
        )
        coEvery { annictRepository.getAllRecords() } returns records
        coEvery { aniListRepository.getMediaByMalId(any()) } returns Result.success(mockk(relaxed = true))


        // When
        viewModel = MyPageViewModel(annictRepository, aniListRepository)

        // Then
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            val finalState = awaitItem()
            assertEquals(false, finalState.isLoading)
            assertEquals(1, finalState.contributionData.size)
            val date = java.time.LocalDate.of(2023, 1, 1)
            assertEquals(2, finalState.contributionData[date])
        }
    }
}
