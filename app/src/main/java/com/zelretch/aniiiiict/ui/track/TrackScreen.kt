package com.zelretch.aniiiiict.ui.track

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiict.ui.details.DetailModal
import com.zelretch.aniiiiict.ui.details.DetailModalViewModel
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
    onNavigateToHistory: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Scaffold(topBar = {
        TrackTopAppBar(
            isFilterVisible = uiState.isFilterVisible,
            onFilterClick = { viewModel.toggleFilterVisibility() },
            onHistoryClick = onNavigateToHistory
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
            onRecordEpisode = onRecordEpisode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackTopAppBar(isFilterVisible: Boolean, onFilterClick: () -> Unit, onHistoryClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "番組一覧",
                style = MaterialTheme.typography.headlineSmall
            )
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
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "履歴"
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
    onRecordEpisode: (String, String, StatusState) -> Unit
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberLazyListState()
                ) {
                    items(items = uiState.programs, key = { it.work.id }) { program ->
                        ProgramCard(
                            programWithWork = program,
                            onRecordEpisode = onRecordEpisode,
                            onShowUnwatchedEpisodes = { viewModel.showUnwatchedEpisodes(program) },
                            uiState = uiState
                        )
                    }
                }
            }
        }
    }

    if (uiState.isDetailModalVisible) {
        val detailModalViewModel = hiltViewModel<DetailModalViewModel>()
        uiState.selectedProgram?.let { program ->
            DetailModal(
                programWithWork = program,
                isLoading = uiState.isLoadingDetail,
                onDismiss = { viewModel.hideDetail() },
                detailModalViewModel,
                onRefresh = onRefresh
            )
        }
    }
}
