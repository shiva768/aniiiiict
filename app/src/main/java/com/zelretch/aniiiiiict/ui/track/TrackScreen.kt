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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.main.components.FilterBar
import com.zelretch.aniiiiiict.ui.main.components.ProgramCard
import com.zelretch.aniiiiiict.ui.unwatched.DetailModal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TrackScreen(
    viewModel: TrackViewModel,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordEpisode: (List<String>, String, StatusState) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = onRefresh
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番組一覧") },
                actions = {
                    // フィルターボタン
                    IconButton(
                        onClick = { viewModel.toggleFilterVisibility() }
                    ) {
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
                            imageVector = Icons.Default.History,
                            contentDescription = "履歴"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            ) {
                Snackbar(
                    modifier = Modifier.testTag("snackbar")
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(uiState.error ?: "")
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { viewModel.refresh() }
                        ) {
                            Text("再読み込み")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.programs,
                        key = { it.work.id }
                    ) { program ->
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

            // プルリフレッシュインジケーター
            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 詳細モーダル
        if (uiState.isDetailModalVisible) {
            uiState.selectedProgram?.let { program ->
                DetailModal(
                    programWithWork = program,
                    isLoading = uiState.isLoadingDetail,
                    onDismiss = { viewModel.hideDetail() },
                    onRecordEpisode = onRecordEpisode,
                    onBulkRecordEpisode = onBulkRecordEpisode
                )
            }
        }
    }
}