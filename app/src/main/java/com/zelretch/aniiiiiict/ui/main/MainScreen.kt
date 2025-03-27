package com.zelretch.aniiiiiict.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults as ChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.ui.components.DatePickerDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.zelretch.aniiiiiict.ui.main.MainUiState
import com.zelretch.aniiiiiict.ui.main.WorkWithCustomDate
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedWorkId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadWorks()
    }

    LaunchedEffect(uiState.authUrl) {
        uiState.authUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aniiiiict") },
                actions = {
                    if (!uiState.isAuthenticated) {
                        TextButton(onClick = { viewModel.startAuth() }) {
                            Text("ログイン")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    val errorMessage = uiState.error ?: "エラーが発生しました"
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    val worksWithCustomDates = uiState.works.map { work ->
                        WorkWithCustomDate(
                            work = work,
                            customStartDate = null // TODO: カスタム開始日を取得
                        )
                    }
                    WorksList(
                        title = "作品一覧",
                        works = worksWithCustomDates,
                        onDateSet = { workId, date ->
                            viewModel.setCustomStartDate(workId, date)
                        },
                        onDateClear = { workId ->
                            viewModel.clearCustomStartDate(workId)
                        },
                        onError = { message ->
                            viewModel.showError(message)
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker && selectedWorkId != null) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.setCustomStartDate(selectedWorkId!!, date)
                showDatePicker = false
            },
            onError = { message ->
                viewModel.showError(message)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun WorksList(
    title: String,
    works: List<WorkWithCustomDate>,
    onDateSet: (Long, LocalDateTime) -> Unit,
    onDateClear: (Long) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(works) { workWithDate ->
                WorkCard(
                    workWithDate = workWithDate,
                    onDateSet = onDateSet,
                    onDateClear = onDateClear,
                    onError = onError
                )
            }
        }
    }
}

@Composable
fun WorkCard(
    workWithDate: WorkWithCustomDate,
    onDateSet: (Long, LocalDateTime) -> Unit,
    onDateClear: (Long) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val work = workWithDate.work
    val isPastWork = work.releasedOn?.let { releasedOn ->
        try {
            val releaseDate = LocalDate.parse(releasedOn)
            ChronoUnit.YEARS.between(releaseDate, LocalDate.now()) > 2
        } catch (e: Exception) {
            false
        }
    } ?: false

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // サムネイル画像
            work.images?.recommendedUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${work.title}のサムネイル",
                    modifier = Modifier
                        .width(120.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // 作品情報
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // タイトル
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // タグ行1: メディア、シーズン、視聴ステータス、チャンネル
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = work.mediaText,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        colors = ChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    work.seasonNameText?.let { season ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = season,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    work.channels?.firstOrNull()?.let { channel ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = ChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = when(work.status?.kind) {
                                    "watching" -> "見てる"
                                    "wanna_watch" -> "見たい"
                                    else -> "未設定"
                                },
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        colors = ChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                // 放送情報
                Column {
                    Text(
                        text = "放送:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isPastWork) {
                        Text(
                            text = "過去作",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = work.channels?.firstOrNull()?.startedAt?.takeIf { it.isNotEmpty() }
                                    ?.let { LocalDateTime.parse(it).format(dateFormatter) }
                                    ?: "未設定",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!isPastWork && work.channels?.firstOrNull()?.startedAt.isNullOrEmpty()) {
                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "開始日を設定",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 次のエピソード情報
                work.nextUnwatchedEpisode?.let { episode ->
                    Column {
                        Text(
                            text = "次のエピソード:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = buildString {
                                append(episode.numberText ?: episode.number ?: "")
                                episode.title?.let { title ->
                                    if (isNotEmpty()) append(" ")
                                    append(title)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // エピソード情報と操作ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { /* TODO: 視聴記録 */ },
                        modifier = Modifier.weight(1f),
                        enabled = work.nextUnwatchedEpisode != null
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "視聴記録",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("視聴記録")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: スキップ */ },
                        enabled = work.nextUnwatchedEpisode != null
                    ) {
                        Icon(
                            Icons.Outlined.SkipNext,
                            contentDescription = "スキップ",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker && !isPastWork) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                try {
                    onDateSet(work.id, date)
                    showDatePicker = false
                } catch (e: Exception) {
                    onError("日付の設定に失敗しました: ${e.message}")
                }
            },
            onError = { message ->
                onError(message)
                showDatePicker = false
            }
        )
    }
} 