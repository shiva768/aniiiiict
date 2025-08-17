package com.zelretch.aniiiiiict.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val annictRepository: AnnictRepository,
    private val aniListRepository: AniListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadAnnictData()
    }

    private fun loadAnnictData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allRecords = annictRepository.getAllRecords()
                val contributionData = processRecordsForContributionGraph(allRecords)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contributionData = contributionData
                    )
                }

                // Now load anilist data
                loadAnilistData(allRecords)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadAnilistData(records: List<Record>) {
        viewModelScope.launch {
            try {
                val malIds = records.mapNotNull { it.work.malAnimeId?.toIntOrNull() }.distinct()
                val genres = mutableListOf<String>()
                val studios = mutableListOf<String>()

                malIds.forEach { malId ->
                    aniListRepository.getMediaByMalId(malId).getOrNull()?.let { media ->
                        media.genres?.let { genres.addAll(it.filterNotNull()) }
                        media.studios?.let { studios.addAll(it.filterNotNull()) }
                    }
                }

                val genreDistribution = genres.groupingBy { it }.eachCount()
                val studioRanking = studios.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }


                _uiState.update {
                    it.copy(
                        genreDistribution = genreDistribution,
                        studioRanking = studioRanking
                    )
                }

            } catch (e: Exception) {
                // Don't update the error state again if it's already set
            }
        }
    }

    private fun processRecordsForContributionGraph(records: List<Record>): Map<java.time.LocalDate, Int> {
        return records
            .groupBy { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) -> dayRecords.size }
    }
}

data class MyPageUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val contributionData: Map<java.time.LocalDate, Int> = emptyMap(),
    val genreDistribution: Map<String, Int> = emptyMap(),
    val studioRanking: List<Pair<String, Int>> = emptyList()
)
