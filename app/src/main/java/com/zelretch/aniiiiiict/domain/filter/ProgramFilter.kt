package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.data.model.ProgramWithWork

class ProgramFilter {
    fun applyFilters(
        programs: List<ProgramWithWork>,
        filterState: FilterState
    ): List<ProgramWithWork> {
        // 基本フィルタリング
        var filtered = programs.filter { program ->
            (filterState.selectedMedia == null || program.work.media == filterState.selectedMedia) &&
                    (filterState.selectedSeason == null || program.work.seasonName == filterState.selectedSeason) &&
                    (filterState.selectedYear == null || program.work.seasonYear == filterState.selectedYear) &&
                    (filterState.selectedChannel == null || program.program.channel.name == filterState.selectedChannel) &&
                    (filterState.selectedStatus == null || program.work.viewerStatusState == filterState.selectedStatus.toString())
        }

        // 検索クエリでフィルタリング
        if (filterState.searchQuery.isNotBlank()) {
            filtered = filtered.filter { program ->
                program.work.title.contains(filterState.searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }

    fun extractAvailableFilters(programs: List<ProgramWithWork>): AvailableFilters {
        val media = programs.mapNotNull { it.work.media }.distinct().sorted()
        val seasons = programs.mapNotNull { it.work.seasonName }.distinct().sorted()
        val years = programs.mapNotNull { it.work.seasonYear }.distinct().sorted()
        val channels = programs.map { it.program.channel.name }.distinct().sorted()

        return AvailableFilters(
            media = media,
            seasons = seasons,
            years = years,
            channels = channels
        )
    }
}

data class AvailableFilters(
    val media: List<String>,
    val seasons: List<String>,
    val years: List<Int>,
    val channels: List<String>
) 