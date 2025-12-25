package com.zelretch.aniiiiict.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    uiState: LibraryUiState,
    onNavigateBack: () -> Unit,
    onShowAnimeDetail: (String) -> Unit
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
            onShowAnimeDetail = onShowAnimeDetail
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenContent(
    modifier: Modifier = Modifier,
    uiState: LibraryUiState,
    viewModel: LibraryViewModel,
    onShowAnimeDetail: (String) -> Unit
) {
    val isRefreshing = uiState.isLoading && uiState.entries.isNotEmpty()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = rememberPullToRefreshState()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isFilterVisible) {
                PastWorksFilterBar(
                    showOnlyPastWorks = uiState.showOnlyPastWorks,
                    onFilterChange = { viewModel.togglePastWorksFilter() }
                )
            }

            if (uiState.isLoading && uiState.entries.isEmpty()) {
                // Shimmer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("読み込み中...")
                }
            } else if (uiState.error != null) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.entries) { entry ->
                        LibraryEntryCard(
                            entry = entry,
                            uiState = uiState,
                            onRecordEpisode = { episodeId, workId, status ->
                                viewModel.recordEpisode(episodeId, workId, status)
                            },
                            onShowAnimeDetail = { onShowAnimeDetail(entry.work.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastWorksFilterBar(showOnlyPastWorks: Boolean, onFilterChange: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = showOnlyPastWorks,
                onClick = { if (!showOnlyPastWorks) onFilterChange() }
            )
            Text("過去作のみ")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = !showOnlyPastWorks,
                onClick = { if (showOnlyPastWorks) onFilterChange() }
            )
            Text("全作品")
        }
    }
}

@Composable
private fun LibraryEntryCard(
    entry: LibraryEntry,
    uiState: LibraryUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onShowAnimeDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = { onShowAnimeDetail() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("program_card_${entry.work.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            WorkInfoRow(
                entry = entry,
                onShowAnimeDetail = onShowAnimeDetail
            )
            Spacer(modifier = Modifier.height(12.dp))
            entry.nextEpisode?.let {
                EpisodeInfoRow(
                    entry = entry,
                    uiState = uiState,
                    onRecordEpisode = onRecordEpisode
                )
            }
        }
    }
}

@Composable
private fun WorkInfoRow(entry: LibraryEntry, onShowAnimeDetail: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        WorkImage(
            imageUrl = entry.work.image?.imageUrl,
            workTitle = entry.work.title
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = entry.work.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("work_title_${entry.work.id}")
                )
                IconButton(
                    onClick = { onShowAnimeDetail() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "詳細を見る",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            WorkTags(entry = entry)
        }
    }
}

@Composable
private fun WorkImage(imageUrl: String?, workTitle: String) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
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

@Composable
private fun WorkTags(entry: LibraryEntry) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            entry.work.media?.let {
                InfoTag(text = it, color = MaterialTheme.colorScheme.primaryContainer)
            }
            entry.work.seasonName?.let {
                InfoTag(text = it.rawValue, color = MaterialTheme.colorScheme.secondaryContainer)
            }
            entry.work.seasonYear?.let {
                InfoTag(
                    text = it.toString() + "年",
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            entry.statusState?.let {
                InfoTag(
                    text = it.rawValue,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun EpisodeInfoRow(
    entry: LibraryEntry,
    uiState: LibraryUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val episodeText = buildString {
                append(
                    entry.nextEpisode?.numberText
                )
                entry.nextEpisode?.title?.let {
                    append(" ")
                    append(it)
                }
            }
            Text(
                text = episodeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        entry.nextEpisode?.let { episode ->
            entry.statusState?.let { statusState ->
                RecordButton(
                    episodeId = episode.id,
                    workId = entry.work.id,
                    status = statusState,
                    uiState = uiState,
                    onRecordEpisode = onRecordEpisode
                )
            }
        }
    }
}

@Composable
private fun RecordButton(
    episodeId: String,
    workId: String,
    status: StatusState,
    uiState: LibraryUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit
) {
    val isRecording = uiState.isRecording
    val recordingSuccess = uiState.recordingSuccess == episodeId
    FilledTonalIconButton(
        onClick = { onRecordEpisode(episodeId, workId, status) },
        modifier = Modifier.size(40.dp),
        enabled = !isRecording && !recordingSuccess,
        colors = if (recordingSuccess) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors()
        }
    ) {
        if (recordingSuccess) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "記録済み",
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "記録する",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InfoTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = contentColorFor(color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
