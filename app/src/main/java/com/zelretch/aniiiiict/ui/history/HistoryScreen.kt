package com.zelretch.aniiiiict.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiict.data.model.Record
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val LOAD_MORE_THRESHOLD = 3
private val PADDING_HORIZONTAL = 16.dp
private val PADDING_VERTICAL = 8.dp
private const val TEXT_ALPHA = 0.7f
private val PADDING_LARGE = 16.dp
private const val COLUMN_WEIGHT = 1f

data class HistoryScreenActions(
    val onNavigateBack: () -> Unit,
    val onRetry: () -> Unit,
    val onDeleteRecord: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onLoadNextPage: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onRecordClick: (Record) -> Unit,
    val onDismissRecordDetail: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(uiState: HistoryUiState, actions: HistoryScreenActions) {
    val listState = rememberLazyListState()
    val shouldLoadNextPage = remember {
        derivedStateOf {
            val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastItem != null && lastItem.index >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadNextPage.value) {
        if (shouldLoadNextPage.value && uiState.hasNextPage && !uiState.isLoading) {
            actions.onLoadNextPage()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "視聴履歴",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = actions.onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                }
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HistorySearchBar(uiState.searchQuery, actions.onSearchQueryChange)
            HistoryContent(uiState, listState, actions)
        }
    }

    // Show detail modal when a record is selected
    if (uiState.isDetailModalVisible && uiState.selectedRecord != null) {
        RecordDetailModal(
            record = uiState.selectedRecord,
            onDismiss = actions.onDismissRecordDetail
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(uiState: HistoryUiState, listState: LazyListState, actions: HistoryScreenActions) {
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = uiState.isLoading,
        onRefresh = actions.onRefresh,
        state = rememberPullToRefreshState()
    ) {
        when {
            uiState.records.isEmpty() &&
                !uiState.isLoading &&
                uiState.error == null &&
                !uiState.hasNextPage
            -> HistoryEmptyState()
            else -> HistoryList(uiState, listState, actions)
        }

        AnimatedVisibility(
            visible = uiState.error != null && uiState.records.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            HistoryErrorState(uiState.error, actions.onRetry)
        }
    }
}

@Composable
private fun HistorySearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PADDING_HORIZONTAL, vertical = PADDING_VERTICAL),
        placeholder = {
            Text(
                text = "作品名・エピソード名で検索",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "クリア",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun HistoryList(uiState: HistoryUiState, listState: LazyListState, actions: HistoryScreenActions) {
    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        items(items = uiState.records, key = { it.id }) { record ->
            RecordItem(
                record = record,
                onDelete = { actions.onDeleteRecord(record.id) },
                onClick = { actions.onRecordClick(record) }
            )
        }
        if (uiState.hasNextPage) {
            item {
                LoadMoreButton(uiState.isLoading, actions.onLoadNextPage)
            }
        }
    }
}

@Composable
private fun LoadMoreButton(isLoading: Boolean, onLoadNextPage: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PADDING_LARGE),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            TextButton(onClick = onLoadNextPage) {
                Text(
                    text = "もっと見る",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "視聴履歴がありません",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "下にスワイプして更新",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = TEXT_ALPHA)
            )
        }
    }
}

@Composable
private fun HistoryErrorState(error: String?, onRetry: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(PADDING_LARGE)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = error ?: "エラーが発生しました",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(PADDING_LARGE))
            TextButton(onClick = onRetry) {
                Text(
                    text = "再試行",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun RecordItem(record: Record, onDelete: () -> Unit, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    val zonedDateTime = record.createdAt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
    val formattedDate = zonedDateTime.format(formatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PADDING_HORIZONTAL,
                vertical = PADDING_VERTICAL
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(COLUMN_WEIGHT)) {
                Text(
                    text = record.work.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${record.episode.formattedNumber} ${record.episode.title ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun RecordDetailModal(record: Record, onDismiss: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
    val zonedDateTime = record.createdAt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
    val formattedDate = zonedDateTime.format(formatter)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "視聴履歴詳細",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "作品名",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.work.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "エピソード",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "EP${record.episode.numberText ?: "?"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = record.episode.title ?: "タイトルなし",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "視聴日時",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                record.comment?.let { comment ->
                    Text(
                        text = "コメント",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                record.rating?.let { rating ->
                    Text(
                        text = "評価",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$rating/5.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "閉じる",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
