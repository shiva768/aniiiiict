package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.type.SeasonName
import java.time.LocalDateTime

class ProgramFilter {
    fun applyFilters(
        programs: List<ProgramWithWork>,
        filterState: FilterState
    ): List<ProgramWithWork> {
        return programs
            .asSequence()
            .filter { program -> applyMediaFilter(program, filterState) }
            .filter { program -> applySeasonFilter(program, filterState) }
            .filter { program -> applyYearFilter(program, filterState) }
            .filter { program -> applyChannelFilter(program, filterState) }
            .filter { program -> applyStatusFilter(program, filterState) }
            .filter { program -> applySearchFilter(program, filterState) }
            .filter { program -> applyAiredFilter(program, filterState) }
            .toList()
            .let { filteredPrograms -> applySortOrder(filteredPrograms, filterState.sortOrder) }
    }

    private fun applyMediaFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        filterState.selectedMedia.isEmpty() || program.work.media in filterState.selectedMedia

    private fun applySeasonFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        filterState.selectedSeason.isEmpty() || program.work.seasonName in filterState.selectedSeason

    private fun applyYearFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        filterState.selectedYear.isEmpty() || program.work.seasonYear in filterState.selectedYear

    private fun applyChannelFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        filterState.selectedChannel.isEmpty() || program.firstProgram.channel.name in filterState.selectedChannel

    private fun applyStatusFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        filterState.selectedStatus.isEmpty() || program.work.viewerStatusState in filterState.selectedStatus

    private fun applySearchFilter(program: ProgramWithWork, filterState: FilterState): Boolean {
        if (filterState.searchQuery.isEmpty()) return true
        val query = filterState.searchQuery.lowercase()
        val title = program.work.title.lowercase()
        return title.contains(query)
    }

    private fun applyAiredFilter(program: ProgramWithWork, filterState: FilterState): Boolean =
        !filterState.showOnlyAired || !program.firstProgram.startedAt.isAfter(LocalDateTime.now())

    private fun applySortOrder(
        programs: List<ProgramWithWork>,
        sortOrder: SortOrder
    ): List<ProgramWithWork> =
        when (sortOrder) {
            SortOrder.START_TIME_ASC -> programs.sortedBy { it.firstProgram.startedAt }
            SortOrder.START_TIME_DESC -> programs.sortedByDescending { it.firstProgram.startedAt }
        }

    fun extractAvailableFilters(programs: List<ProgramWithWork>): AvailableFilters {
        val media = programs.mapNotNull { it.work.media }.distinct().sorted()

        // シーズンの並び順を定義
        val seasonOrder = mapOf(
            SeasonName.WINTER to 0,
            SeasonName.SPRING to 1,
            SeasonName.SUMMER to 2,
            SeasonName.AUTUMN to 3
        )

        // シーズンをカスタム順序でソート
        val seasons = programs.mapNotNull { it.work.seasonName }
            .distinct()
            .sortedWith(compareBy { seasonOrder[it] ?: Int.MAX_VALUE })

        val years = programs.mapNotNull { it.work.seasonYear }.distinct().sorted()
        val channels = programs.map { it.firstProgram.channel.name }.distinct().sorted()

        return AvailableFilters(media, seasons, years, channels)
    }
}

data class AvailableFilters(
    val media: List<String>,
    val seasons: List<SeasonName>,
    val years: List<Int>,
    val channels: List<String>
) 