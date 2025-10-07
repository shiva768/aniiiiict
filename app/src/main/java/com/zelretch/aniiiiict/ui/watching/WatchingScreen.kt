package com.zelretch.aniiiiict.ui.watching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiict.data.model.LibraryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchingScreen(
    viewModel: WatchingViewModel,
    uiState: WatchingUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WatchingTopAppBar(
                isFilterVisible = uiState.isFilterVisible,
                onFilterClick = { viewModel.toggleFilterVisibility() },
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        WatchingScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchingTopAppBar(
    isFilterVisible: Boolean,
    onFilterClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "視聴中作品",
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
private fun WatchingScreenContent(
    modifier: Modifier = Modifier,
    uiState: WatchingUiState,
    viewModel: WatchingViewModel
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
                    items(uiState.entries) { entry ->
                        LibraryEntryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PastWorksFilterBar(
    showOnlyPastWorks: Boolean,
    onFilterChange: () -> Unit
) {
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
private fun LibraryEntryCard(entry: LibraryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.work.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val seasonInfo = buildString {
                entry.work.seasonYear?.let { append("${it}年") }
                entry.work.seasonName?.let { append(it.rawValue) }
                entry.work.media?.let { 
                    if (isNotEmpty()) append(" ")
                    append(it)
                }
            }
            if (seasonInfo.isNotEmpty()) {
                Text(
                    text = seasonInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.nextEpisode?.let { episode ->
                val episodeText = buildString {
                    append("次：")
                    episode.numberText?.let { append(it) } ?: episode.number?.let { append("第${it}話") }
                    episode.title?.let { 
                        if (isNotEmpty()) append(" ")
                        append("「${it}」")
                    }
                }
                Text(
                    text = episodeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
