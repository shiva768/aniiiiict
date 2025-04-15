package com.zelretch.aniiiiiict.ui.track.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
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
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.track.TrackUiState
import java.time.format.DateTimeFormatter

@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onShowUnwatchedEpisodes: (ProgramWithWork) -> Unit,
    uiState: TrackUiState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("program_card_${programWithWork.work.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 上部：作品情報と画像
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 画像（左側）
                val imageUrl = programWithWork.work.image?.imageUrl

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = programWithWork.work.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
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
                        modifier = Modifier.testTag("work_title_${programWithWork.work.id}")
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
                                    text = it.name,
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
                                    text = it.toString(),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }

                            // チャンネル名
                            InfoTag(
                                text = programWithWork.firstProgram.channel.name,
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
                        append(programWithWork.firstProgram.episode.numberText ?: "?")
                        programWithWork.firstProgram.episode.title?.let {
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
                        text = programWithWork.firstProgram.startedAt.format(
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
                                programWithWork.firstProgram.episode.id,
                                programWithWork.work.id,
                                programWithWork.work.viewerStatusState
                            )
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = !uiState.isLoading && !uiState.isRecording && uiState.recordingSuccess != programWithWork.firstProgram.episode.id,
                        colors = if (uiState.recordingSuccess == programWithWork.firstProgram.episode.id) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        }
                    ) {
                        if (uiState.recordingSuccess == programWithWork.firstProgram.episode.id) {
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

                    // 未視聴エピソードボタン
                    FilledTonalIconButton(
                        onClick = { onShowUnwatchedEpisodes(programWithWork) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "未視聴エピソード",
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