package com.zelretch.aniiiiict.ui.track.components

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.filter.SortOrder

private data class FilterChipActions(
    val onMediaClick: () -> Unit,
    val onSeasonClick: () -> Unit,
    val onYearClick: () -> Unit,
    val onChannelClick: () -> Unit,
    val onStatusClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(filterState: FilterState, filterOptions: FilterOptions, onFilterChange: (FilterState) -> Unit) {
    var showMediaDialog by remember { mutableStateOf(false) }
    var showSeasonDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTextField(
                searchQuery = filterState.searchQuery,
                onQueryChange = { onFilterChange(filterState.copy(searchQuery = it)) }
            )

            FilterChips(
                filterState = filterState,
                actions = FilterChipActions(
                    onMediaClick = { showMediaDialog = true },
                    onSeasonClick = { showSeasonDialog = true },
                    onYearClick = { showYearDialog = true },
                    onChannelClick = { showChannelDialog = true },
                    onStatusClick = { showStatusDialog = true }
                )
            )

            DisplayAndSortOptions(
                filterState = filterState,
                onFilterChange = onFilterChange
            )
        }
    }

    if (showMediaDialog) {
        MediaFilterDialog(filterOptions, filterState, onFilterChange) { showMediaDialog = false }
    }
    if (showSeasonDialog) {
        SeasonFilterDialog(filterOptions, filterState, onFilterChange) { showSeasonDialog = false }
    }
    if (showYearDialog) {
        YearFilterDialog(filterOptions, filterState, onFilterChange) { showYearDialog = false }
    }
    if (showChannelDialog) {
        ChannelFilterDialog(filterOptions, filterState, onFilterChange) { showChannelDialog = false }
    }
    if (showStatusDialog) {
        StatusFilterDialog(filterState, onFilterChange) { showStatusDialog = false }
    }
}

@Composable
private fun SearchTextField(searchQuery: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "作品名やチャンネル名で検索",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "クリア",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(filterState: FilterState, actions: FilterChipActions) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = filterState.selectedMedia.isNotEmpty(),
            onClick = actions.onMediaClick,
            label = { Text("メディア") },
            leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) }
        )
        FilterChip(
            selected = filterState.selectedSeason.isNotEmpty(),
            onClick = actions.onSeasonClick,
            label = { Text("シーズン") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
        )
        FilterChip(
            selected = filterState.selectedYear.isNotEmpty(),
            onClick = actions.onYearClick,
            label = { Text("年") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
        )
        FilterChip(
            selected = filterState.selectedChannel.isNotEmpty(),
            onClick = actions.onChannelClick,
            label = { Text("チャンネル") },
            leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null) }
        )
        FilterChip(
            selected = filterState.selectedStatus.isNotEmpty(),
            onClick = actions.onStatusClick,
            label = { Text("ステータス") },
            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
        )
    }
}

@Composable
private fun DisplayAndSortOptions(filterState: FilterState, onFilterChange: (FilterState) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = filterState.showOnlyAired,
            onCheckedChange = { onFilterChange(filterState.copy(showOnlyAired = it)) }
        )
        Text(text = "放送済", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("並び順：")
            FilterChip(
                selected = filterState.sortOrder == SortOrder.START_TIME_ASC,
                onClick = { onFilterChange(filterState.copy(sortOrder = SortOrder.START_TIME_ASC)) },
                label = { Text("昇順") }
            )
            FilterChip(
                selected = filterState.sortOrder == SortOrder.START_TIME_DESC,
                onClick = { onFilterChange(filterState.copy(sortOrder = SortOrder.START_TIME_DESC)) },
                label = { Text("降順") }
            )
        }
    }
}

@Composable
private fun MediaFilterDialog(
    filterOptions: FilterOptions,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSelectionDialog(
        title = "メディアを選択",
        items = filterOptions.media,
        selectedItems = filterState.selectedMedia,
        onItemSelected = { media ->
            val newSelection = filterState.selectedMedia.toMutableSet()
            if (media in newSelection) newSelection.remove(media) else newSelection.add(media)
            onFilterChange(filterState.copy(selectedMedia = newSelection))
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun SeasonFilterDialog(
    filterOptions: FilterOptions,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSelectionDialog(
        title = "シーズンを選択",
        items = filterOptions.seasons.map { it.name },
        selectedItems = filterState.selectedSeason.map { it.name }.toSet(),
        onItemSelected = { seasonStr ->
            val season = SeasonName.valueOf(seasonStr)
            val newSelection = filterState.selectedSeason.toMutableSet()
            if (season in newSelection) newSelection.remove(season) else newSelection.add(season)
            onFilterChange(filterState.copy(selectedSeason = newSelection))
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun YearFilterDialog(
    filterOptions: FilterOptions,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSelectionDialog(
        title = "年を選択",
        items = filterOptions.years.map { it.toString() },
        selectedItems = filterState.selectedYear.map { it.toString() }.toSet(),
        onItemSelected = { yearStr ->
            val year = yearStr.toIntOrNull() ?: return@FilterSelectionDialog
            val newSelection = filterState.selectedYear.toMutableSet()
            if (year in newSelection) newSelection.remove(year) else newSelection.add(year)
            onFilterChange(filterState.copy(selectedYear = newSelection))
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun ChannelFilterDialog(
    filterOptions: FilterOptions,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSelectionDialog(
        title = "チャンネルを選択",
        items = filterOptions.channels,
        selectedItems = filterState.selectedChannel,
        onItemSelected = { channel ->
            val newSelection = filterState.selectedChannel.toMutableSet()
            if (channel in newSelection) newSelection.remove(channel) else newSelection.add(channel)
            onFilterChange(filterState.copy(selectedChannel = newSelection))
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun StatusFilterDialog(filterState: FilterState, onFilterChange: (FilterState) -> Unit, onDismiss: () -> Unit) {
    FilterSelectionDialog(
        title = "ステータスを選択",
        items = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH).map { it.name },
        selectedItems = filterState.selectedStatus.map { it.name }.toSet(),
        onItemSelected = { statusStr ->
            val status = StatusState.valueOf(statusStr)
            val newSelection = filterState.selectedStatus.toMutableSet()
            if (status in newSelection) newSelection.remove(status) else newSelection.add(status)
            onFilterChange(filterState.copy(selectedStatus = newSelection))
        },
        onDismiss = onDismiss
    )
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
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    FilterChip(
                        selected = item in selectedItems,
                        onClick = {
                            onItemSelected(item)
                        },
                        label = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "閉じる",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
