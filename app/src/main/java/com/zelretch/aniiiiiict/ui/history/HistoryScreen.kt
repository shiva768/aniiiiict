package com.zelretch.aniiiiiict.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zelretch.aniiiiiict.data.model.Record
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = onRefresh
    )

    val listState = rememberLazyListState()
    val shouldLoadNextPage = remember {
        derivedStateOf {
            val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastItem != null && lastItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadNextPage.value) {
        if (shouldLoadNextPage.value && uiState.hasNextPage && !uiState.isLoading) {
            onLoadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("視聴履歴") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            // 検索バー
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("作品名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (uiState.records.isEmpty() && !uiState.isLoading && uiState.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("視聴履歴がありません")
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
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    items(
                        items = uiState.records,
                        key = { it.id }
                    ) { record ->
                        RecordItem(
                            record = record,
                            onDelete = { onDeleteRecord(record.id) }
                        )
                    }

                    if (uiState.hasNextPage) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator()
                                } else {
                                    TextButton(
                                        onClick = onLoadNextPage
                                    ) {
                                        Text("もっと見る")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null && uiState.records.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "エラーが発生しました",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(onClick = onRetry) {
                            Text("再試行")
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordItem(
    record: Record,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    val formattedDate = record.createdAt.format(formatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側: タイトル、エピソード名、日時
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.work.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "EP${record.episode.numberText} ${record.episode.title}",
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右側: ゴミ箱アイコン
            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 