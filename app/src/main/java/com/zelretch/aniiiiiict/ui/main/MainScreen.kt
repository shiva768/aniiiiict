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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
                if (uiState.programs.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "プログラムがありません",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
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
                        enabled = !uiState.isRecording,
                        colors = if (uiState.recordingSuccess == programWithWork.program.episode.id) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        if (uiState.isRecording && uiState.recordingSuccess == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.recordingSuccess == programWithWork.program.episode.id) {
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

                    Spacer(modifier = Modifier.width(8.dp))

                    // スキップボタン
                    OutlinedIconButton(
                        onClick = { /* スキップ処理 */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "スキップ",
                            modifier = Modifier.size(20.dp)
                        )
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
    onFilterChange: (String?, String?, Int?, String?, StatusState?, String) -> Unit
) {
    var showMediaFilter by remember { mutableStateOf(false) }
    var showSeasonFilter by remember { mutableStateOf(false) }
    var showYearFilter by remember { mutableStateOf(false) }
    var showChannelFilter by remember { mutableStateOf(false) }
    var showStatusFilter by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(filterState.searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // 検索バー
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onFilterChange(
                    filterState.selectedMedia,
                    filterState.selectedSeason,
                    filterState.selectedYear,
                    filterState.selectedChannel,
                    filterState.selectedStatus,
                    it
                )
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("作品名やチャンネル名で検索") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // フィルターチップ
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // メディアフィルター
            FilterChip(
                selected = filterState.selectedMedia != null,
                onClick = { showMediaFilter = true },
                label = { Text(filterState.selectedMedia ?: "メディア") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // シーズンフィルター（春夏秋冬）
            FilterChip(
                selected = filterState.selectedSeason != null,
                onClick = { showSeasonFilter = true },
                label = { Text(filterState.selectedSeason ?: "シーズン") },
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // 年フィルター
            FilterChip(
                selected = filterState.selectedYear != null,
                onClick = { showYearFilter = true },
                label = { Text(filterState.selectedYear?.toString() ?: "年") },
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // チャンネルフィルター
            FilterChip(
                selected = filterState.selectedChannel != null,
                onClick = { showChannelFilter = true },
                label = { Text(filterState.selectedChannel ?: "チャンネル") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // ステータスフィルター
            FilterChip(
                selected = filterState.selectedStatus != null,
                onClick = { showStatusFilter = true },
                label = { Text(filterState.selectedStatus?.toString() ?: "ステータス") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }

    // メディアフィルターダイアログ
    if (showMediaFilter) {
        AlertDialog(
            onDismissRequest = { showMediaFilter = false },
            title = { Text("メディアを選択") },
            text = {
                LazyColumn {
                    item {
                        FilterChip(
                            selected = filterState.selectedMedia == null,
                            onClick = {
                                onFilterChange(
                                    null,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showMediaFilter = false
                            },
                            label = { Text("すべて") }
                        )
                    }
                    items(availableMedia) { media ->
                        FilterChip(
                            selected = media == filterState.selectedMedia,
                            onClick = {
                                onFilterChange(
                                    media,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showMediaFilter = false
                            },
                            label = { Text(media) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMediaFilter = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // シーズンフィルターダイアログ（春夏秋冬）
    if (showSeasonFilter) {
        AlertDialog(
            onDismissRequest = { showSeasonFilter = false },
            title = { Text("シーズンを選択") },
            text = {
                LazyColumn {
                    item {
                        FilterChip(
                            selected = filterState.selectedSeason == null,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    null,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showSeasonFilter = false
                            },
                            label = { Text("すべて") }
                        )
                    }
                    items(availableSeasons) { season ->
                        FilterChip(
                            selected = season == filterState.selectedSeason,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    season,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showSeasonFilter = false
                            },
                            label = { Text(season) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeasonFilter = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // 年フィルターダイアログ
    if (showYearFilter) {
        AlertDialog(
            onDismissRequest = { showYearFilter = false },
            title = { Text("年を選択") },
            text = {
                LazyColumn {
                    item {
                        FilterChip(
                            selected = filterState.selectedYear == null,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    null,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showYearFilter = false
                            },
                            label = { Text("すべて") }
                        )
                    }
                    items(availableYears) { year ->
                        FilterChip(
                            selected = year == filterState.selectedYear,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    year,
                                    filterState.selectedChannel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showYearFilter = false
                            },
                            label = { Text(year.toString()) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearFilter = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // チャンネルフィルターダイアログ
    if (showChannelFilter) {
        AlertDialog(
            onDismissRequest = { showChannelFilter = false },
            title = { Text("チャンネルを選択") },
            text = {
                LazyColumn {
                    item {
                        FilterChip(
                            selected = filterState.selectedChannel == null,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    null,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showChannelFilter = false
                            },
                            label = { Text("すべて") }
                        )
                    }
                    items(availableChannels) { channel ->
                        FilterChip(
                            selected = channel == filterState.selectedChannel,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    channel,
                                    filterState.selectedStatus,
                                    searchQuery
                                )
                                showChannelFilter = false
                            },
                            label = { Text(channel) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelFilter = false }) {
                    Text("閉じる")
                }
            }
        )
    }

    // ステータスフィルターダイアログ
    if (showStatusFilter) {
        AlertDialog(
            onDismissRequest = { showStatusFilter = false },
            title = { Text("ステータスを選択") },
            text = {
                LazyColumn {
                    item {
                        FilterChip(
                            selected = filterState.selectedStatus == null,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    null,
                                    searchQuery
                                )
                                showStatusFilter = false
                            },
                            label = { Text("すべて") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterState.selectedStatus == StatusState.WANNA_WATCH,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    StatusState.WANNA_WATCH,
                                    searchQuery
                                )
                                showStatusFilter = false
                            },
                            label = { Text("見たい") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterState.selectedStatus == StatusState.WATCHING,
                            onClick = {
                                onFilterChange(
                                    filterState.selectedMedia,
                                    filterState.selectedSeason,
                                    filterState.selectedYear,
                                    filterState.selectedChannel,
                                    StatusState.WATCHING,
                                    searchQuery
                                )
                                showStatusFilter = false
                            },
                            label = { Text("見てる") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusFilter = false }) {
                    Text("閉じる")
                }
            }
        )
    }
} 