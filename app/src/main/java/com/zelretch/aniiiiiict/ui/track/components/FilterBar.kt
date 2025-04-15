package com.zelretch.aniiiiiict.ui.track.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import kotlin.reflect.KFunction8

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    filterState: FilterState,
    availableMedia: List<String>,
    availableSeasons: List<SeasonName>,
    availableYears: List<Int>,
    availableChannels: List<String>,
    onFilterChange: KFunction8<Set<String>, Set<SeasonName>, Set<Int>, Set<String>, Set<StatusState>, String, Boolean, SortOrder, Unit>
) {
    var showMediaDialog by remember { mutableStateOf(false) }
    var showSeasonDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    // フィルター更新のヘルパー関数
    fun updateFilter(
        selectedMedia: Set<String> = filterState.selectedMedia,
        selectedSeason: Set<SeasonName> = filterState.selectedSeason,
        selectedYear: Set<Int> = filterState.selectedYear,
        selectedChannel: Set<String> = filterState.selectedChannel,
        selectedStatus: Set<StatusState> = filterState.selectedStatus,
        searchQuery: String = filterState.searchQuery,
        showOnlyAired: Boolean = filterState.showOnlyAired,
        sortOrder: SortOrder = filterState.sortOrder
    ) {
        onFilterChange(
            selectedMedia,
            selectedSeason,
            selectedYear,
            selectedChannel,
            selectedStatus,
            searchQuery,
            showOnlyAired,
            sortOrder
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 検索フィールド
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { query ->
                    updateFilter(searchQuery = query)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("作品名やチャンネル名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                singleLine = true
            )

            // フィルターボタン
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // メディアフィルター
                FilterChip(
                    selected = filterState.selectedMedia.isNotEmpty(),
                    onClick = { showMediaDialog = true },
                    label = { Text("メディア") },
                    leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) }
                )

                // シーズンフィルター
                FilterChip(
                    selected = filterState.selectedSeason.isNotEmpty(),
                    onClick = { showSeasonDialog = true },
                    label = { Text("シーズン") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // 年フィルター
                FilterChip(
                    selected = filterState.selectedYear.isNotEmpty(),
                    onClick = { showYearDialog = true },
                    label = { Text("年") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // チャンネルフィルター
                FilterChip(
                    selected = filterState.selectedChannel.isNotEmpty(),
                    onClick = { showChannelDialog = true },
                    label = { Text("チャンネル") },
                    leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null) }
                )

                // ステータスフィルター
                FilterChip(
                    selected = filterState.selectedStatus.isNotEmpty(),
                    onClick = { showStatusDialog = true },
                    label = { Text("ステータス") },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }

            // 放送済みのみ表示チェックボックス
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = filterState.showOnlyAired,
                    onCheckedChange = { checked ->
                        updateFilter(showOnlyAired = checked)
                    }
                )
                Text(
                    text = "放送済",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                // 並び順
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("並び順：")
                    FilterChip(
                        selected = filterState.sortOrder == SortOrder.START_TIME_ASC,
                        onClick = {
                            updateFilter(sortOrder = SortOrder.START_TIME_ASC)
                        },
                        label = { Text("昇順") }
                    )
                    FilterChip(
                        selected = filterState.sortOrder == SortOrder.START_TIME_DESC,
                        onClick = {
                            updateFilter(sortOrder = SortOrder.START_TIME_DESC)
                        },
                        label = { Text("降順") }
                    )
                }
            }
        }
    }

    // フィルターダイアログの表示
    if (showMediaDialog) {
        FilterSelectionDialog(
            title = "メディアを選択",
            items = availableMedia,
            selectedItems = filterState.selectedMedia,
            onItemSelected = { media ->
                val newSelection = filterState.selectedMedia.toMutableSet()
                if (media in newSelection) {
                    newSelection.remove(media)
                } else {
                    newSelection.add(media)
                }
                updateFilter(selectedMedia = newSelection)
            },
            onDismiss = { showMediaDialog = false }
        )
    }

    if (showSeasonDialog) {
        FilterSelectionDialog(
            title = "シーズンを選択",
            items = availableSeasons.map { it.name },
            selectedItems = filterState.selectedSeason.map { it.name }.toSet(),
            onItemSelected = { seasonStr ->
                val season = SeasonName.valueOf(seasonStr)
                val newSelection = filterState.selectedSeason.toMutableSet()
                if (season in newSelection) {
                    newSelection.remove(season)
                } else {
                    newSelection.add(season)
                }
                updateFilter(selectedSeason = newSelection)
            },
            onDismiss = { showSeasonDialog = false }
        )
    }

    if (showYearDialog) {
        FilterSelectionDialog(
            title = "年を選択",
            items = availableYears.map { it.toString() },
            selectedItems = filterState.selectedYear.map { it.toString() }.toSet(),
            onItemSelected = { yearStr ->
                val year = yearStr.toIntOrNull() ?: return@FilterSelectionDialog
                val newSelection = filterState.selectedYear.toMutableSet()
                if (year in newSelection) {
                    newSelection.remove(year)
                } else {
                    newSelection.add(year)
                }
                updateFilter(selectedYear = newSelection)
            },
            onDismiss = { showYearDialog = false }
        )
    }

    if (showChannelDialog) {
        FilterSelectionDialog(
            title = "チャンネルを選択",
            items = availableChannels,
            selectedItems = filterState.selectedChannel,
            onItemSelected = { channel ->
                val newSelection = filterState.selectedChannel.toMutableSet()
                if (channel in newSelection) {
                    newSelection.remove(channel)
                } else {
                    newSelection.add(channel)
                }
                updateFilter(selectedChannel = newSelection)
            },
            onDismiss = { showChannelDialog = false }
        )
    }

    if (showStatusDialog) {
        FilterSelectionDialog(
            title = "ステータスを選択",
            items = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH).map { it.name },
            selectedItems = filterState.selectedStatus.map { it.name }.toSet(),
            onItemSelected = { statusStr ->
                val status = StatusState.valueOf(statusStr)
                val newSelection = filterState.selectedStatus.toMutableSet()
                if (status in newSelection) {
                    newSelection.remove(status)
                } else {
                    newSelection.add(status)
                }
                updateFilter(selectedStatus = newSelection)
            },
            onDismiss = { showStatusDialog = false }
        )
    }
}

@Composable
fun FilterSelectionDialog(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    FilterChip(
                        selected = item in selectedItems,
                        onClick = { onItemSelected(item) },
                        label = { Text(item) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
} 