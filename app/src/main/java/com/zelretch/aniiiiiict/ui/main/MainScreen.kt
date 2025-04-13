package com.zelretch.aniiiiiict.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import com.zelretch.aniiiiiict.type.StatusState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImageLoad: (Int, String) -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
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
                // 空のリスト状態でもPullRefreshが機能するように、常にLazyColumnを表示
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    if (uiState.programs.isEmpty() && !uiState.isLoading) {
                        item {
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
                        items(
                            items = uiState.programs,
                            key = { it.program.annictId },
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
                                    onImageLoad = {
                                        program.work.image?.recommendedImageUrl?.let { imageUrl ->
                                            onImageLoad(program.program.annictId, imageUrl)
                                        }
                                    },
                                    onRecordEpisode = onRecordEpisode,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onImageLoad: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    uiState: MainUiState
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("program_card_${programWithWork.program.annictId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ヘッダー：画像と基本情報
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 画像（左側）
                val imageUrl =
                    programWithWork.work.image?.recommendedImageUrl.takeIf { !it.isNullOrEmpty() }
                        ?: programWithWork.work.image?.facebookOgImageUrl.takeIf { !it.isNullOrEmpty() }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (imageUrl != null) {
                        // 1つのBoxで画像とローディング状態を処理
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = programWithWork.work.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // 常に画像URLがあれば保存を試みる（画像表示の成否に関わらず）
                        onImageLoad()
                    } else {
                        // 画像がない場合はプレースホルダー
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右側の情報
                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    val workTitle = programWithWork.work.title
                    Text(
                        text = workTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("work_title_${programWithWork.work.annictId}")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // タグ情報（縦に並べる）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // メディアタイプ
                        programWithWork.work.media?.let {
                            InfoTag(text = it, color = MaterialTheme.colorScheme.primaryContainer)
                        }

                        // シーズンと年（横に並べる）
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            // シーズン名
                            programWithWork.work.seasonName?.let {
                                InfoTag(
                                    text = it,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }

                            // 年
                            programWithWork.work.seasonYear?.let {
                                InfoTag(
                                    text = it.toString() + "年",
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                        }

                        // 視聴ステータスとチャンネルを横に並べる
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            // 視聴ステータス
                            programWithWork.work.viewerStatusState.let {
                                InfoTag(
                                    text = it,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }

                            // チャンネル名
                            InfoTag(
                                text = programWithWork.program.channel.name,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // エピソード情報
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // エピソード番号とタイトル
                    val episodeText = buildString {
                        append("Episode ")
                        append(programWithWork.program.episode.numberText ?: "?")
                        programWithWork.program.episode.title?.let {
                            append(" ")
                            append(it)
                        }
                    }

                    Text(
                        text = episodeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // 放送日時
                    Text(
                        text = programWithWork.program.startedAt.format(
                            DateTimeFormatter.ofPattern(
                                "yyyy/MM/dd HH:mm"
                            )
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // アクションボタン
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 記録ボタン
                    FilledTonalIconButton(
                        onClick = {
                            onRecordEpisode(
                                programWithWork.program.episode.id,
                                programWithWork.work.id,
                                StatusState.valueOf(programWithWork.work.viewerStatusState)
                            )
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = !uiState.isLoading && !uiState.isRecording && uiState.recordingSuccess != programWithWork.program.episode.id,
                        colors = if (uiState.recordingSuccess == programWithWork.program.episode.id) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        if (uiState.recordingSuccess == programWithWork.program.episode.id) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "記録済み",
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "記録する",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = contentColorFor(color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    filterState: FilterState,
    availableMedia: List<String>,
    availableSeasons: List<String>,
    availableYears: List<Int>,
    availableChannels: List<String>,
    onFilterChange: (
        selectedMedia: Set<String>,
        selectedSeason: Set<String>,
        selectedYear: Set<Int>,
        selectedChannel: Set<String>,
        selectedStatus: Set<StatusState>,
        searchQuery: String,
        showOnlyAired: Boolean,
        sortOrder: SortOrder
    ) -> Unit
) {
    var showMediaDialog by remember { mutableStateOf(false) }
    var showSeasonDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 検索フィールド
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { query ->
                    onFilterChange(
                        filterState.selectedMedia,
                        filterState.selectedSeason,
                        filterState.selectedYear,
                        filterState.selectedChannel,
                        filterState.selectedStatus,
                        query,
                        filterState.showOnlyAired,
                        filterState.sortOrder
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("作品名やチャンネル名で検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                singleLine = true
            )

            // フィルターボタン
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // メディアフィルター
                FilterChip(
                    selected = filterState.selectedMedia.isNotEmpty(),
                    onClick = { showMediaDialog = true },
                    label = { Text("メディア") },
                    leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) }
                )

                // シーズンフィルター
                FilterChip(
                    selected = filterState.selectedSeason.isNotEmpty(),
                    onClick = { showSeasonDialog = true },
                    label = { Text("シーズン") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // 年フィルター
                FilterChip(
                    selected = filterState.selectedYear.isNotEmpty(),
                    onClick = { showYearDialog = true },
                    label = { Text("年") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                // チャンネルフィルター
                FilterChip(
                    selected = filterState.selectedChannel.isNotEmpty(),
                    onClick = { showChannelDialog = true },
                    label = { Text("チャンネル") },
                    leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null) }
                )

                // ステータスフィルター
                FilterChip(
                    selected = filterState.selectedStatus.isNotEmpty(),
                    onClick = { showStatusDialog = true },
                    label = { Text("ステータス") },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }

            // 放送済みのみ表示チェックボックス
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = filterState.showOnlyAired,
                    onCheckedChange = { checked ->
                        onFilterChange(
                            filterState.selectedMedia,
                            filterState.selectedSeason,
                            filterState.selectedYear,
                            filterState.selectedChannel,
                            filterState.selectedStatus,
                            filterState.searchQuery,
                            checked,
                            filterState.sortOrder
                        )
                    }
                )
                Text(
                    text = "放送済",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                // 並び順
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("並び順：")
                    FilterChip(
                        selected = filterState.sortOrder == SortOrder.START_TIME_ASC,
                        onClick = {
                            onFilterChange(
                                filterState.selectedMedia,
                                filterState.selectedSeason,
                                filterState.selectedYear,
                                filterState.selectedChannel,
                                filterState.selectedStatus,
                                filterState.searchQuery,
                                filterState.showOnlyAired,
                                SortOrder.START_TIME_ASC
                            )
                        },
                        label = { Text("昇順") }
                    )
                    FilterChip(
                        selected = filterState.sortOrder == SortOrder.START_TIME_DESC,
                        onClick = {
                            onFilterChange(
                                filterState.selectedMedia,
                                filterState.selectedSeason,
                                filterState.selectedYear,
                                filterState.selectedChannel,
                                filterState.selectedStatus,
                                filterState.searchQuery,
                                filterState.showOnlyAired,
                                SortOrder.START_TIME_DESC
                            )
                        },
                        label = { Text("降順") }
                    )
                }
            }
        }
    }

    // メディア選択ダイアログ
    if (showMediaDialog) {
        AlertDialog(
            onDismissRequest = { showMediaDialog = false },
            title = { Text("メディアを選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableMedia.forEach { media ->
                        FilterChip(
                            selected = media in filterState.selectedMedia,
                            onClick = {
                                val newSelection = filterState.selectedMedia.toMutableSet()
                                if (media in newSelection) {
                                    newSelection.remove(media)
                                } else {
                                    newSelection.add(media)
                                }
                                onFilterChange(
                                    newSelection,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    filterState.searchQuery,
                                    filterState.showOnlyAired,
                                    filterState.sortOrder
                                )
                            },
                            label = { Text(media) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // シーズン選択ダイアログ
    if (showSeasonDialog) {
        AlertDialog(
            onDismissRequest = { showSeasonDialog = false },
            title = { Text("シーズンを選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableSeasons.forEach { season ->
                        FilterChip(
                            selected = season in filterState.selectedSeason,
                            onClick = {
                                val newSelection = filterState.selectedSeason.toMutableSet()
                                if (season in newSelection) {
                                    newSelection.remove(season)
                                } else {
                                    newSelection.add(season)
                                }
                                onFilterChange(
                                    filterState.selectedMedia,
                                    newSelection,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    filterState.searchQuery,
                                    filterState.showOnlyAired,
                                    filterState.sortOrder
                                )
                            },
                            label = { Text(season) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeasonDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // 年選択ダイアログ
    if (showYearDialog) {
        AlertDialog(
            onDismissRequest = { showYearDialog = false },
            title = { Text("年を選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableYears.forEach { year ->
                        FilterChip(
                            selected = year in filterState.selectedYear,
                            onClick = {
                                val newSelection = filterState.selectedYear.toMutableSet()
                                if (year in newSelection) {
                                    newSelection.remove(year)
                                } else {
                                    newSelection.add(year)
                                }
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    newSelection,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    filterState.searchQuery,
                                    filterState.showOnlyAired,
                                    filterState.sortOrder
                                )
                            },
                            label = { Text(year.toString()) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // チャンネル選択ダイアログ
    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = { Text("チャンネルを選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableChannels.forEach { channel ->
                        FilterChip(
                            selected = channel in filterState.selectedChannel,
                            onClick = {
                                val newSelection = filterState.selectedChannel.toMutableSet()
                                if (channel in newSelection) {
                                    newSelection.remove(channel)
                                } else {
                                    newSelection.add(channel)
                                }
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    newSelection,
                                    filterState.selectedStatus,
                                    filterState.searchQuery,
                                    filterState.showOnlyAired,
                                    filterState.sortOrder
                                )
                            },
                            label = { Text(channel) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // ステータス選択ダイアログ
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("ステータスを選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(StatusState.WATCHING, StatusState.WANNA_WATCH).forEach { status ->
                        FilterChip(
                            selected = status in filterState.selectedStatus,
                            onClick = {
                                val newSelection = filterState.selectedStatus.toMutableSet()
                                if (status in newSelection) {
                                    newSelection.remove(status)
                                } else {
                                    newSelection.add(status)
                                }
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    newSelection,
                                    filterState.searchQuery,
                                    filterState.showOnlyAired,
                                    filterState.sortOrder
                                )
                            },
                            label = { Text(status.name) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }
} 