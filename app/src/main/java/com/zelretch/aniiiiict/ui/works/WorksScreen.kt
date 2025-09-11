package com.zelretch.aniiiiict.ui.works

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiict.ui.details.DetailModal
import com.zelretch.aniiiiict.ui.details.DetailModalViewModel
import com.zelretch.aniiiiict.ui.works.components.FilterBar
import com.zelretch.aniiiiict.ui.works.components.FilterOptions
import com.zelretch.aniiiiict.ui.works.components.ProgramCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksScreen(
    viewModel: WorksViewModel,
    uiState: WorksUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onRefresh: () -> Unit = {}
) {
    WorksScreenContent(
        uiState = uiState,
        viewModel = viewModel,
        onRefresh = onRefresh,
        onRecordEpisode = onRecordEpisode
    )
}

@Composable
private fun WorksSnackbarHost(
    uiState: WorksUiState,
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
                TextButton(onClick = onDismissFinale) { Text("いいえ") }
                TextButton(onClick = onConfirmFinale) { Text("はい") }
            }
        }) {
            Text("このタイトルはエピソード${uiState.showFinaleConfirmationForEpisodeNumber}が最終話の可能性があります。\n視聴済みにしますか？")
        }
    } else if (uiState.error != null) {
        Snackbar(modifier = Modifier.testTag("snackbar")) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(uiState.error)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Text("再読み込み") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorksScreenContent(
    modifier: Modifier = Modifier,
    uiState: WorksUiState,
    viewModel: WorksViewModel,
    onRefresh: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit
) {
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = uiState.isLoading,
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
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
