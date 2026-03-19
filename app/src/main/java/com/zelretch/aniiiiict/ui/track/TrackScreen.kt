package com.zelretch.aniiiiict.ui.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.track.components.FilterBar
import com.zelretch.aniiiiict.ui.track.components.FilterOptions
import com.zelretch.aniiiiict.ui.track.components.ProgramCard
import com.zelretch.aniiiiict.ui.track.components.ProgramCardPlaceholder

private const val SHIMMER_ITEM_COUNT = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: TrackViewModel,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onRefresh: () -> Unit = {},
    onMenuClick: () -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit = {}
) {
    Scaffold(topBar = {
        TrackTopAppBar(
            isFilterVisible = uiState.isFilterVisible,
            onFilterClick = { viewModel.toggleFilterVisibility() },
            onMenuClick = onMenuClick
        )
    }, snackbarHost = {
        TrackSnackbarHost(
            uiState = uiState,
            onConfirmFinale = { viewModel.confirmWatchedStatus() },
            onDismissFinale = { viewModel.dismissFinaleConfirmation() },
            onRefresh = { viewModel.refresh() }
        )
    }) { paddingValues ->
        TrackScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            viewModel = viewModel,
            onRefresh = onRefresh,
            onRecordEpisode = onRecordEpisode,
            onShowAnimeDetail = onShowAnimeDetail
        )
    }
}

@Composable
private fun ProgramListContent(
    uiState: TrackUiState,
    onToggleSearchOnlyMode: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onShowUnwatchedEpisodes: (ProgramWithWork) -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit
) {
    val showSearchOnlySuggestion = !uiState.isLoading &&
        uiState.programs.isEmpty() &&
        uiState.filterState.searchQuery.isNotEmpty() &&
        uiState.filterState.hasActiveNonSearchFilters() &&
        !uiState.isSearchOnlyMode

    if (showSearchOnlySuggestion) {
        SearchOnlySuggestion(
            onSearchOnly = onToggleSearchOnlyMode,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (uiState.isSearchOnlyMode) {
        SearchOnlyModeIndicator(onRestore = onToggleSearchOnlyMode)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState()
    ) {
        items(items = uiState.programs, key = { it.work.id }) { program ->
            ProgramCard(
                programWithWork = program,
                onRecordEpisode = onRecordEpisode,
                onShowUnwatchedEpisodes = { onShowUnwatchedEpisodes(program) },
                onShowAnimeDetail = onShowAnimeDetail,
                uiState = uiState
            )
        }
    }
}

@Composable
private fun SearchOnlyModeIndicator(onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "他のフィルターを一時的にオフにしています",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRestore) {
            Text(
                text = "元に戻す",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun SearchOnlySuggestion(onSearchOnly: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "一致する作品が見つかりませんでした",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onSearchOnly,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "他のフィルターを一時的にオフにして検索",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackTopAppBar(isFilterVisible: Boolean, onFilterClick: () -> Unit, onMenuClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "放送スケジュール",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "メニュー"
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
private fun TrackSnackbarHost(
    uiState: TrackUiState,
    onConfirmFinale: () -> Unit,
    onDismissFinale: () -> Unit,
    onRefresh: () -> Unit
) {
    // Render snackbars directly based on UI state so tests can observe them
    if (uiState.showFinaleConfirmationForWorkId != null) {
        Snackbar(modifier = Modifier.testTag("finale_confirmation_snackbar"), action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Place "いいえ" first, then "はい" to avoid any layout quirks causing mis-clicks
                TextButton(onClick = onDismissFinale) {
                    Text(
                        text = "いいえ",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                TextButton(onClick = onConfirmFinale) {
                    Text(
                        text = "はい",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }) {
            Text(
                text = "このタイトルはエピソード${uiState.showFinaleConfirmationForEpisodeNumber}が最終話の可能性があります。\n視聴済みにしますか？",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else if (uiState.error != null) {
        Snackbar(modifier = Modifier.testTag("snackbar")) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) {
                    Text(
                        text = "再読み込み",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackScreenContent(
    modifier: Modifier = Modifier,
    uiState: TrackUiState,
    viewModel: TrackViewModel,
    onRefresh: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit
) {
    val isRefreshing = uiState.isLoading && uiState.programs.isNotEmpty()
    val isInitialLoad = uiState.isLoading && uiState.programs.isEmpty()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 検索ワードのインジケーター
            if (uiState.filterState.searchQuery.isNotEmpty()) {
                Text(
                    text = "🔍 ${uiState.filterState.searchQuery}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (uiState.isFilterVisible) {
                val filterOptions = FilterOptions(
                    media = uiState.availableMedia,
                    seasons = uiState.availableSeasons,
                    years = uiState.availableYears,
                    channels = uiState.availableChannels
                )
                FilterBar(
                    filterOptions = filterOptions,
                    filterState = uiState.filterState,
                    onFilterChange = viewModel::updateFilter
                )
            }

            if (isInitialLoad) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(SHIMMER_ITEM_COUNT) {
                        ProgramCardPlaceholder()
                    }
                }
            } else {
                ProgramListContent(
                    uiState = uiState,
                    onToggleSearchOnlyMode = { viewModel.toggleSearchOnlyMode() },
                    onRecordEpisode = onRecordEpisode,
                    onShowUnwatchedEpisodes = { viewModel.showUnwatchedEpisodes(it) },
                    onShowAnimeDetail = onShowAnimeDetail
                )
            }
        }
    }

    if (uiState.isDetailModalVisible) {
        val broadcastEpisodeModalViewModel = hiltViewModel<BroadcastEpisodeModalViewModel>()
        uiState.selectedProgram?.let { program ->
            BroadcastEpisodeModal(
                programWithWork = program,
                isLoading = uiState.isLoadingDetail,
                onDismiss = { viewModel.hideDetail() },
                broadcastEpisodeModalViewModel,
                onRefresh = onRefresh
            )
        }
    }
}
