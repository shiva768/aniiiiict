package com.zelretch.aniiiiict.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenContent(modifier: Modifier = Modifier, uiState: LibraryUiState, viewModel: LibraryViewModel) {
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
                // 初回ローディング表示
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

    // WatchingEpisodeModalを表示
    if (uiState.isDetailModalVisible) {
        val watchingEpisodeModalViewModel = hiltViewModel<WatchingEpisodeModalViewModel>()
        uiState.selectedEntry?.let { entry ->
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = { viewModel.hideDetail() },
                watchingEpisodeModalViewModel,
                onRefresh = { viewModel.refresh() }
            )
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
            // 作品情報セクション
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

            // 次のエピソード情報セクション
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

private fun StatusState.toJapaneseLabel(): String = when (this) {
    StatusState.WATCHING -> "視聴中"
    StatusState.WANNA_WATCH -> "見たい"
    StatusState.WATCHED -> "見た"
    StatusState.STOP_WATCHING -> "中止"
    StatusState.ON_HOLD -> "保留"
    else -> toString()
}
