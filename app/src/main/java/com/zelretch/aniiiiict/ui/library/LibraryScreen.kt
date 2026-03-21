package com.zelretch.aniiiiict.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.ui.common.components.toJapaneseLabel
import com.zelretch.aniiiiict.ui.track.components.InfoTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, uiState: LibraryUiState, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            LibraryTopAppBar(
                isFilterVisible = uiState.isFilterVisible,
                onFilterClick = { viewModel.toggleFilterVisibility() },
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        LibraryScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopAppBar(isFilterVisible: Boolean, onFilterClick: () -> Unit, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "ライブラリ",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "フィルター",
                    tint = if (isFilterVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    )
}

@Composable
private fun LibraryScreenContent(modifier: Modifier = Modifier, uiState: LibraryUiState, viewModel: LibraryViewModel) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isFilterVisible) {
                LibraryFilterBar(
                    filterState = uiState.filterState,
                    availableMedia = uiState.availableMedia,
                    availableStatuses = uiState.availableStatuses,
                    availableYears = uiState.availableYears,
                    availableSeasons = uiState.availableSeasons,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onMediaFilterChange = { viewModel.toggleMediaFilter(it) },
                    onStatusFilterChange = { viewModel.toggleStatusFilter(it) },
                    onYearFilterChange = { viewModel.toggleYearFilter(it) },
                    onSeasonFilterChange = { viewModel.toggleSeasonFilter(it) },
                    onSortOrderChange = { viewModel.updateSortOrder(it) }
                )
            }

            when {
                uiState.isSyncing -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("更新中のためしばらくお待ちください")
                    }
                }
                uiState.isLoading && uiState.entries.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("読み込み中...")
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entries, key = { it.work.id }) { entry ->
                            LibraryEntryCard(
                                entry = entry,
                                onClick = { viewModel.showDetail(entry) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isDetailModalVisible) {
        val watchingEpisodeModalViewModel = hiltViewModel<WatchingEpisodeModalViewModel>()
        uiState.selectedEntry?.let { entry ->
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = { viewModel.hideDetail() },
                watchingEpisodeModalViewModel,
                onRefresh = { viewModel.onEntryUpdated(entry.id) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterBar(
    filterState: LibraryFilterState,
    availableMedia: List<String>,
    availableStatuses: List<StatusState>,
    availableYears: List<Int>,
    availableSeasons: List<SeasonName>,
    onSearchQueryChange: (String) -> Unit,
    onMediaFilterChange: (String) -> Unit,
    onStatusFilterChange: (StatusState) -> Unit,
    onYearFilterChange: (Int) -> Unit,
    onSeasonFilterChange: (SeasonName) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("タイトル検索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (availableStatuses.isNotEmpty()) {
            FilterChipRow(label = "ステータス") {
                availableStatuses.forEach { status ->
                    FilterChip(
                        selected = status in filterState.selectedStatuses,
                        onClick = { onStatusFilterChange(status) },
                        label = { Text(status.toJapaneseLabel()) }
                    )
                }
            }
        }
        if (availableYears.isNotEmpty()) {
            FilterChipRow(label = "年") {
                availableYears.forEach { year ->
                    FilterChip(
                        selected = year in filterState.selectedYears,
                        onClick = { onYearFilterChange(year) },
                        label = { Text("${year}年") }
                    )
                }
            }
        }
        if (availableSeasons.isNotEmpty()) {
            FilterChipRow(label = "クール") {
                availableSeasons.forEach { season ->
                    FilterChip(
                        selected = season in filterState.selectedSeasons,
                        onClick = { onSeasonFilterChange(season) },
                        label = { Text(season.toJapaneseLabel()) }
                    )
                }
            }
        }
        if (availableMedia.isNotEmpty()) {
            FilterChipRow(label = "メディア") {
                availableMedia.forEach { media ->
                    FilterChip(
                        selected = media in filterState.selectedMedia,
                        onClick = { onMediaFilterChange(media) },
                        label = { Text(media) }
                    )
                }
            }
        }
        FilterChipRow(label = "並び順") {
            FilterChip(
                selected = filterState.sortOrder == LibrarySortOrder.SEASON_DESC,
                onClick = { onSortOrderChange(LibrarySortOrder.SEASON_DESC) },
                label = { Text("新しい順") }
            )
            FilterChip(
                selected = filterState.sortOrder == LibrarySortOrder.SEASON_ASC,
                onClick = { onSortOrderChange(LibrarySortOrder.SEASON_ASC) },
                label = { Text("古い順") }
            )
            FilterChip(
                selected = filterState.sortOrder == LibrarySortOrder.TITLE_ASC,
                onClick = { onSortOrderChange(LibrarySortOrder.TITLE_ASC) },
                label = { Text("タイトル順") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(label: String, chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips()
        }
    }
}

@Composable
private fun LibraryEntryCard(entry: LibraryEntry, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryWorkImage(
                    imageUrl = entry.work.image?.imageUrl,
                    workTitle = entry.work.title
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = entry.work.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    entry.statusState?.let { status ->
                        InfoTag(
                            text = status.toJapaneseLabel(),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                    val seasonMeta = buildString {
                        entry.work.seasonYear?.let { append("${it}年") }
                        entry.work.seasonName?.let { append(it.rawValue) }
                        entry.work.media?.let {
                            if (isNotEmpty()) append(" · ")
                            append(it)
                        }
                    }
                    if (seasonMeta.isNotEmpty()) {
                        Text(
                            text = seasonMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            entry.nextEpisode?.let { episode ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val episodeText = buildString {
                    episode.numberText?.let { append(it) } ?: episode.number?.let { append("第${it}話") }
                    episode.title?.let {
                        if (isNotEmpty()) append("「$it」")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = episodeText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryWorkImage(imageUrl: String?, workTitle: String) {
    Box(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = workTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun SeasonName.toJapaneseLabel(): String = when (this) {
    SeasonName.SPRING -> "春"
    SeasonName.SUMMER -> "夏"
    SeasonName.AUTUMN -> "秋"
    SeasonName.WINTER -> "冬"
    else -> rawValue
}
