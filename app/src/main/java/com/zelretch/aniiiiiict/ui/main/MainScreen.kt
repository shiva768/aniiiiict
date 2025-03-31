package com.zelretch.aniiiiiict.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.ui.components.DatePickerDialog
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onProgramClick: (ProgramWithWork) -> Unit,
    onDateChange: (LocalDateTime) -> Unit,
    onImageLoad: (Int, String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーメッセージを表示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
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

    Scaffold(topBar = {
        TopAppBar(title = { Text("Aniiiiict") }, actions = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select date")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        // コンテンツの記憶（再コンポーズを最小限に）
        val programs = remember(uiState.programs) { uiState.programs }
        val isAuthenticating = remember(uiState.isAuthenticating) { uiState.isAuthenticating }
        val isLoading = remember(uiState.isLoading) { uiState.isLoading }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                                onClick = { onProgramClick(program) },
                                onImageLoad = {
                                    program.work.image?.recommendedImageUrl?.let { imageUrl ->
                                        onImageLoad(program.program.annictId, imageUrl)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // 認証中の表示（半透明のオーバーレイ）- アニメーション付き
            if (isAuthenticating || authAnimValue.value > 0) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .alpha(authAnimValue.value), // アニメーション適用
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Surface(
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
            // データがない場合のメッセージ（ローディング中でなく、認証中でもない場合）
            else if (!isLoading && !isAuthenticating && programs.isEmpty()) {
                Text(
                    text = "番組がありません",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(onDismiss = { showDatePicker = false }, onDateSelected = { date ->
            onDateChange(date.atTime(LocalTime.now()))
            showDatePicker = false
        }, onError = {
            // エラー処理は省略
            showDatePicker = false
        })
    }
}

@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onClick: () -> Unit,
    onImageLoad: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = programWithWork.work.image?.recommendedImageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = programWithWork.work.title,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { onImageLoad() }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = programWithWork.work.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = programWithWork.work.media ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Episode ${programWithWork.program.episode.numberText ?: "?"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = programWithWork.program.startedAt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 