package com.zelretch.aniiiiiict.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.main.components.FilterBar
import com.zelretch.aniiiiiict.ui.main.components.ProgramCard
import com.zelretch.aniiiiiict.ui.unwatched.UnwatchedEpisodesModal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordEpisode: (List<String>, String, StatusState) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = onRefresh
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プログラム一覧") },
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
            SnackbarHost(hostState = remember { SnackbarHostState() }) {
                if (uiState.error != null) {
                    Snackbar(
                        modifier = Modifier.testTag("error_snackbar"),
                        action = {
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text("再試行")
                            }
                        }
                    ) {
                        Text(uiState.error!!)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // フィルターバー（アニメーション付きで表示/非表示）
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.isFilterVisible,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() +
                        androidx.compose.animation.shrinkVertically()
            ) {
                FilterBar(
                    filterState = uiState.filterState,
                    availableMedia = uiState.availableMedia,
                    availableSeasons = uiState.availableSeasons,
                    availableYears = uiState.availableYears,
                    availableChannels = uiState.availableChannels,
                    onFilterChange = viewModel::updateFilter
                )
            }

            // プログラム一覧
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (uiState.programs.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "プログラムがありません",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "下にスワイプして更新",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
                        items(
                            items = uiState.programs,
                            key = { it.firstProgram.id },
                            contentType = { "program" }
                        ) { program ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = androidx.compose.animation.fadeIn() +
                                        androidx.compose.animation.expandVertically(),
                                exit = androidx.compose.animation.fadeOut() +
                                        androidx.compose.animation.shrinkVertically()
                            ) {
                                ProgramCard(
                                    programWithWork = program,
                                    onRecordEpisode = onRecordEpisode,
                                    onShowUnwatchedEpisodes = { viewModel.showUnwatchedEpisodes(it) },
                                    uiState = uiState
                                )
                            }
                        }
                    }
                }

                // ローディングインジケーター
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }

                PullRefreshIndicator(
                    refreshing = uiState.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // 未視聴エピソードモーダル
        if (uiState.isUnwatchedEpisodesModalVisible) {
            uiState.selectedProgram?.let { program ->
                UnwatchedEpisodesModal(
                    programWithWork = program,
                    isLoading = uiState.isLoadingUnwatchedEpisodes,
                    onDismiss = { viewModel.hideUnwatchedEpisodes() },
                    onRecordEpisode = onRecordEpisode,
                    onBulkRecordEpisode = onBulkRecordEpisode
                )
            }
        }
    }
} 