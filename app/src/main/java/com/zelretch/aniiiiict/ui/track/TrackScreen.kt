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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.zelretch.aniiiiict.ui.track.components.FilterBottomSheet
import com.zelretch.aniiiiict.ui.track.components.FilterOptions
import com.zelretch.aniiiiict.ui.track.components.FilterSummaryRow
import com.zelretch.aniiiiict.ui.track.components.ProgramCard
import com.zelretch.aniiiiict.ui.track.components.ProgramCardPlaceholder
import com.zelretch.aniiiiict.ui.track.components.appliedCount

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
            appliedCount = uiState.filterState.appliedCount(),
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
    onBulkRecordUpTo: (ProgramWithWork, Int) -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit
) {
    // showOnlyAired（放送済のみ表示）も検索結果を絞る要因になるため、他のフィルターが
    // 未選択でもこれがONなら「検索専用モード」の提案対象に含める
    val showSearchOnlySuggestion = !uiState.isLoading &&
        uiState.programs.isEmpty() &&
        uiState.filterState.searchQuery.isNotEmpty() &&
        (uiState.filterState.hasActiveNonSearchFilters() || uiState.filterState.showOnlyAired) &&
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
                onBulkRecordUpTo = { index -> onBulkRecordUpTo(program, index) },
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
private fun TrackTopAppBar(appliedCount: Int, onFilterClick: () -> Unit, onMenuClick: () -> Unit) {
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
                BadgedBox(badge = {
                    if (appliedCount > 0) {
                        Badge { Text(appliedCount.toString()) }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "フィルター",
                        tint = if (appliedCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
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
    // Snackbar アクション色：ダーク背景(inverseSurface)でのコントラスト確保に inversePrimary を使う
    val snackbarActionColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.inversePrimary
    )
    if (uiState.showFinaleConfirmationForWorkId != null) {
        Snackbar(modifier = Modifier.testTag("finale_confirmation_snackbar"), action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Place "いいえ" first, then "はい" to avoid any layout quirks causing mis-clicks
                TextButton(onClick = onDismissFinale, colors = snackbarActionColors) {
                    Text(
                        text = "いいえ",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                TextButton(onClick = onConfirmFinale, colors = snackbarActionColors) {
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
                TextButton(onClick = onRefresh, colors = snackbarActionColors) {
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
            // 使用中のみ表示される検索・フィルターの要約行（非使用時はゼロ占有）
            FilterSummaryRow(
                filterState = uiState.filterState,
                onOpenSheet = { viewModel.toggleFilterVisibility() },
                onClearSearch = { viewModel.updateFilter(uiState.filterState.copy(searchQuery = "")) }
            )

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
                    onBulkRecordUpTo = { program, index -> viewModel.bulkRecordUpTo(program, index) },
                    onShowAnimeDetail = onShowAnimeDetail
                )
            }
        }
    }

    if (uiState.isFilterVisible) {
        FilterBottomSheet(
            filterState = uiState.filterState,
            filterOptions = FilterOptions(
                media = uiState.availableMedia,
                seasons = uiState.availableSeasons,
                years = uiState.availableYears,
                channels = uiState.availableChannels
            ),
            previewCount = { viewModel.previewCount(it) },
            onApply = {
                viewModel.updateFilter(it)
                viewModel.toggleFilterVisibility()
            },
            onDismiss = { viewModel.toggleFilterVisibility() }
        )
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
