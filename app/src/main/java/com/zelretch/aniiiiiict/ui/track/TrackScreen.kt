package com.zelretch.aniiiiiict.ui.track

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.ui.details.DetailModal
import com.zelretch.aniiiiiict.ui.details.DetailModalViewModel
import com.zelretch.aniiiiiict.ui.track.components.FilterBar
import com.zelretch.aniiiiiict.ui.track.components.ProgramCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: TrackViewModel,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("番組一覧") }, actions = {
            // フィルターボタン
            IconButton(onClick = { viewModel.toggleFilterVisibility() }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "フィルター",
                    tint = if (uiState.isFilterVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // 履歴画面へのナビゲーションボタン
            IconButton(
                onClick = onNavigateToHistory
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "マイページ"
                )
            }
        })
    }, snackbarHost = {
        SnackbarHost(hostState = remember { SnackbarHostState() }) {
            if (uiState.showFinaleConfirmationForWorkId != null) {
                Snackbar(modifier = Modifier.testTag("finale_confirmation_snackbar"), action = {
                    TextButton(onClick = { viewModel.confirmWatchedStatus() }) {
                        Text("はい")
                    }
                    TextButton(onClick = { viewModel.dismissFinaleConfirmation() }) {
                        Text("いいえ")
                    }
                }) {
                    Text(
                        "このタイトルはエピソード${uiState.showFinaleConfirmationForEpisodeNumber}が最終話の可能性があります、視聴済みにしますか？"
                    )
                }
            } else if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.testTag("snackbar")
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(uiState.error ?: "")
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("再読み込み")
                        }
                    }
                }
            }
        }
    }) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            state = pullToRefreshState
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // フィルターバー
                if (uiState.isFilterVisible) {
                    FilterBar(
                        availableMedia = uiState.availableMedia,
                        availableSeasons = uiState.availableSeasons,
                        availableYears = uiState.availableYears,
                        availableChannels = uiState.availableChannels,
                        filterState = uiState.filterState,
                        onFilterChange = viewModel::updateFilter
                    )
                }

                // プログラム一覧
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
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

            // ローディングインジケーター
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // 詳細モーダル
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
}
