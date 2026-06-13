package com.zelretch.aniiiiict.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.zelretch.aniiiiict.ui.common.components.toStatusColor
import com.zelretch.aniiiiict.ui.track.components.FilterSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    uiState: LibraryUiState,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
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
            viewModel = viewModel,
            onNavigateToDetail = onNavigateToDetail
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
private fun LibraryScreenContent(
    modifier: Modifier = Modifier,
    uiState: LibraryUiState,
    viewModel: LibraryViewModel,
    onNavigateToDetail: (String) -> Unit = {}
) {
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
                                isRecording = uiState.recordingEntryId == entry.id,
                                onClick = { viewModel.showDetail(entry) },
                                onRecordNextEpisode = { viewModel.recordNextEpisode(entry) }
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
                onRefresh = { viewModel.onEntryUpdated(entry.id) },
                onNavigateToDetail = { onNavigateToDetail(entry.work.id) }
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
    var showStatusDialog by remember { mutableStateOf(false) }
    var showSeasonDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showMediaDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("タイトル検索", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (availableStatuses.isNotEmpty()) {
                    FilterChip(
                        selected = filterState.selectedStatuses.isNotEmpty(),
                        onClick = { showStatusDialog = true },
                        label = { Text("ステータス") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                    )
                }
                if (availableSeasons.isNotEmpty()) {
                    FilterChip(
                        selected = filterState.selectedSeasons.isNotEmpty(),
                        onClick = { showSeasonDialog = true },
                        label = { Text("クール") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                }
                if (availableYears.isNotEmpty()) {
                    FilterChip(
                        selected = filterState.selectedYears.isNotEmpty(),
                        onClick = { showYearDialog = true },
                        label = { Text("年") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                }
                if (availableMedia.isNotEmpty()) {
                    FilterChip(
                        selected = filterState.selectedMedia.isNotEmpty(),
                        onClick = { showMediaDialog = true },
                        label = { Text("メディア") },
                        leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("並び順：", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
    }

    if (showStatusDialog) {
        val statusMap = availableStatuses.associateBy { it.toJapaneseLabel() }
        FilterSelectionDialog(
            title = "ステータスを選択",
            items = availableStatuses.map { it.toJapaneseLabel() },
            selectedItems = filterState.selectedStatuses.map { it.toJapaneseLabel() }.toSet(),
            onItemSelected = { label ->
                statusMap[label]?.let { onStatusFilterChange(it) }
            },
            onDismiss = { showStatusDialog = false }
        )
    }
    if (showSeasonDialog) {
        FilterSelectionDialog(
            title = "クールを選択",
            items = availableSeasons.map { it.toJapaneseLabel() },
            selectedItems = filterState.selectedSeasons.map { it.toJapaneseLabel() }.toSet(),
            onItemSelected = { label ->
                availableSeasons.find { it.toJapaneseLabel() == label }?.let { onSeasonFilterChange(it) }
            },
            onDismiss = { showSeasonDialog = false }
        )
    }
    if (showYearDialog) {
        FilterSelectionDialog(
            title = "年を選択",
            items = availableYears.map { "${it}年" },
            selectedItems = filterState.selectedYears.map { "${it}年" }.toSet(),
            onItemSelected = { label ->
                label.removeSuffix("年").toIntOrNull()?.let { onYearFilterChange(it) }
            },
            onDismiss = { showYearDialog = false }
        )
    }
    if (showMediaDialog) {
        FilterSelectionDialog(
            title = "メディアを選択",
            items = availableMedia,
            selectedItems = filterState.selectedMedia,
            onItemSelected = { onMediaFilterChange(it) },
            onDismiss = { showMediaDialog = false }
        )
    }
}

@Composable
internal fun LibraryEntryCard(
    entry: LibraryEntry,
    isRecording: Boolean,
    onClick: () -> Unit,
    onRecordNextEpisode: () -> Unit
) {
    val statusColor = entry.statusState?.toStatusColor()
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // ステータス色レール（5dp）
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor ?: MaterialTheme.colorScheme.outlineVariant)
            )
            Column(modifier = Modifier.weight(1f)) {
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
                            StatusChip(text = status.toJapaneseLabel(), color = statusColor ?: status.toStatusColor())
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

                LibraryNextEpisodeRow(
                    entry = entry,
                    statusColor = statusColor,
                    isRecording = isRecording,
                    onRecordNextEpisode = onRecordNextEpisode
                )
            }
        }
    }
}

@Composable
private fun LibraryNextEpisodeRow(
    entry: LibraryEntry,
    statusColor: Color?,
    isRecording: Boolean,
    onRecordNextEpisode: () -> Unit
) {
    val episode = entry.nextEpisode
    // 視聴済み（WATCHED）作品はボタンなしの静的表示
    val isWatched = entry.statusState == StatusState.WATCHED
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (episode == null || isWatched) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = statusColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "すべて視聴済み",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val episodeText = buildString {
                episode.numberText?.let { append(it) } ?: episode.number?.let { append("第${it}話") }
                episode.title?.let {
                    if (isNotEmpty()) append("「$it」")
                }
            }
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onRecordNextEpisode,
                enabled = !isRecording,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "見た", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
