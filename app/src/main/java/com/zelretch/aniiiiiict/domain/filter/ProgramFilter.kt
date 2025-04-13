package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.type.StatusState
import java.time.LocalDateTime

class ProgramFilter {
    fun applyFilters(
        programs: List<ProgramWithWork>,
        filterState: FilterState
    ): List<ProgramWithWork> {
        return programs
            .filter { program ->
                // メディアフィルター
                if (filterState.selectedMedia.isNotEmpty() && program.work.media !in filterState.selectedMedia) {
                    return@filter false
                }

                // シーズンフィルター
                if (filterState.selectedSeason.isNotEmpty() && program.work.seasonName !in filterState.selectedSeason) {
                    return@filter false
                }

                // 年フィルター
                if (filterState.selectedYear.isNotEmpty() && program.work.seasonYear !in filterState.selectedYear) {
                    return@filter false
                }

                // チャンネルフィルター
                if (filterState.selectedChannel.isNotEmpty() && program.program.channel.name !in filterState.selectedChannel) {
                    return@filter false
                }

                // ステータスフィルター
                if (filterState.selectedStatus.isNotEmpty() && StatusState.valueOf(program.work.viewerStatusState) !in filterState.selectedStatus) {
                    return@filter false
                }

                // 検索フィルター
                if (filterState.searchQuery.isNotEmpty()) {
                    val query = filterState.searchQuery.lowercase()
                    val title = program.work.title.lowercase()
                    val channel = program.program.channel.name.lowercase()
                    if (!title.contains(query) && !channel.contains(query)) {
                        return@filter false
                    }
                }

                // 放送済みのみ表示フィルター
                if (filterState.showOnlyAired && program.program.startedAt.isAfter(LocalDateTime.now())) {
                    return@filter false
                }

                true
            }
            .let { filteredPrograms ->
                when (filterState.sortOrder) {
                    SortOrder.START_TIME_ASC -> filteredPrograms.sortedBy { it.program.startedAt }
                    SortOrder.START_TIME_DESC -> filteredPrograms.sortedByDescending { it.program.startedAt }
                }
            }
    }

    fun extractAvailableFilters(programs: List<ProgramWithWork>): AvailableFilters {
        val media = programs.mapNotNull { it.work.media }.distinct().sorted()

        // シーズンの並び順を定義
        val seasonOrder = mapOf(
            "WINTER" to 0,
            "SPRING" to 1,
            "SUMMER" to 2,
            "AUTUMN" to 3
        )

        // シーズンをカスタム順序でソート
        val seasons = programs.mapNotNull { it.work.seasonName }
            .distinct()
            .sortedWith(compareBy { seasonOrder[it] ?: Int.MAX_VALUE })
        
        val years = programs.mapNotNull { it.work.seasonYear }.distinct().sorted()
        val channels = programs.map { it.program.channel.name }.distinct().sorted()

        return AvailableFilters(media, seasons, years, channels)
    }
}

data class AvailableFilters(
    val media: List<String>,
    val seasons: List<String>,
    val years: List<Int>,
    val channels: List<String>
) 