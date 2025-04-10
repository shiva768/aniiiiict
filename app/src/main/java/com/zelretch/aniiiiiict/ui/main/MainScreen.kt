package com.zelretch.aniiiiiict.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onImageLoad: (Int, String) -> Unit,
    onRecordEpisode: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onRefresh: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = onRefresh
    )

    // エラーメッセージを表示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // 記録成功メッセージを表示
    LaunchedEffect(uiState.recordingSuccess) {
        uiState.recordingSuccess?.let {
            snackbarHostState.showSnackbar("エピソードを記録しました")
        }
    }

    // 認証状態のアニメーション値
    val authAnimValue = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (uiState.isAuthenticating) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300,
            easing = androidx.compose.animation.core.FastOutLinearInEasing
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Aniiiiict") }, actions = {
                IconButton(onClick = { onNavigateToHistory() }) {
                    Icon(Icons.Default.History, contentDescription = "履歴")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            // コンテンツの記憶（再コンポーズを最小限に）
            val programs = remember(uiState.programs) { uiState.programs }
            val isAuthenticating = remember(uiState.isAuthenticating) { uiState.isAuthenticating }
            val isLoading = remember(uiState.isLoading) { uiState.isLoading }

            // プログラム一覧表示（ローディング中でも表示を継続）
            if (programs.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    state = androidx.compose.foundation.lazy.rememberLazyListState()
                ) {
                    items(
                        items = programs,
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
                                onRecordEpisode = { onRecordEpisode(program.program.episode.id) },
                                uiState = uiState
                            )
                        }
                    }
                }
            } else {
                // 番組リストが空の場合でもBoxを表示してPullToRefreshを有効にする
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "視聴中のアニメはありません",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "下にスワイプして更新",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 認証中の表示（半透明のオーバーレイ）- アニメーション付き
            if (isAuthenticating || authAnimValue.value > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .alpha(authAnimValue.value), // アニメーション適用
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.padding(16.dp))
                            Text(
                                "認証中...",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "ブラウザで認証を完了してください",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            // ローディング表示（空のリストの場合のみ）
            else if (isLoading && programs.isEmpty()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onImageLoad: () -> Unit,
    onRecordEpisode: (String) -> Unit,
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
                            programWithWork.work.viewerStatusState?.let {
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
                        onClick = { onRecordEpisode(programWithWork.program.episode.id) },
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