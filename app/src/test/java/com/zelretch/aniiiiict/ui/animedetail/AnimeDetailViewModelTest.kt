package com.zelretch.aniiiiict.ui.animedetail

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class AnimeDetailViewModelTest {

    private lateinit var getAnimeDetailUseCase: GetAnimeDetailUseCase
    private lateinit var viewModel: AnimeDetailViewModel

    @Before
    fun setup() {
        getAnimeDetailUseCase = mockk()
        viewModel = AnimeDetailViewModel(getAnimeDetailUseCase)
    }

    @Test
    fun `loadAnimeDetail should update state with loading then success`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val animeDetailInfo = createSampleAnimeDetailInfo()

        coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(animeDetailInfo)

        // When
        viewModel.loadAnimeDetail(programWithWork)

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.animeDetailInfo)
        assertEquals("テストアニメ", state.animeDetailInfo?.work?.title)
        assertEquals(null, state.error)
    }

    @Test
    fun `loadAnimeDetail should update state with loading then error`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val errorMessage = "API Error"

        coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.loadAnimeDetail(programWithWork)

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(null, state.animeDetailInfo)
        assertTrue(state.error?.contains(errorMessage) == true)
    }

    @Test
    fun `initial state should be loading`() {
        // When
        val initialState = viewModel.state.value

        // Then
        assertTrue(initialState.isLoading)
        assertEquals(null, initialState.animeDetailInfo)
        assertEquals(null, initialState.error)
    }

    private fun createSampleProgramWithWork(): ProgramWithWork {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        val episode = Episode(
            id = "episode-id",
            number = 1,
            numberText = "1",
            title = "第1話"
        )

        val channel = Channel(name = "テストチャンネル")

        val program = Program(
            id = "program-id",
            startedAt = LocalDateTime.now(),
            channel = channel,
            episode = episode
        )

        return ProgramWithWork(
            work = work,
            programs = listOf(program),
            firstProgram = program
        )
    }

    private fun createSampleAnimeDetailInfo(): AnimeDetailInfo {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        val malInfo = MyAnimeListResponse(
            id = 12345,
            mediaType = "tv",
            numEpisodes = 24,
            status = "currently_airing",
            broadcast = null,
            mainPicture = null
        )

        return AnimeDetailInfo(
            work = work,
            programs = null,
            seriesList = null,
            malInfo = malInfo,
            episodeCount = 24,
            imageUrl = "https://example.com/image.jpg",
            officialSiteUrl = "https://example.com/official",
            wikipediaUrl = "https://ja.wikipedia.org/wiki/テストアニメ"
        )
    }
}
