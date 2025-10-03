package com.zelretch.aniiiiict.ui.track.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
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
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.track.TrackUiState
import java.time.format.DateTimeFormatter

@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onShowUnwatchedEpisodes: (ProgramWithWork) -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit,
    uiState: TrackUiState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("program_card_${programWithWork.work.id}")
            .clickable { onShowUnwatchedEpisodes(programWithWork) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            WorkInfoRow(
                programWithWork = programWithWork,
                onShowAnimeDetail = onShowAnimeDetail
            )
            Spacer(modifier = Modifier.height(12.dp))
            EpisodeInfoRow(
                programWithWork = programWithWork,
                uiState = uiState,
                onRecordEpisode = onRecordEpisode
            )
        }
    }
}

@Composable
private fun WorkInfoRow(programWithWork: ProgramWithWork, onShowAnimeDetail: (ProgramWithWork) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        WorkImage(
            imageUrl = programWithWork.work.image?.imageUrl,
            workTitle = programWithWork.work.title
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = programWithWork.work.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("work_title_${programWithWork.work.id}")
                )
                IconButton(
                    onClick = { onShowAnimeDetail(programWithWork) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "詳細を見る",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            WorkTags(programWithWork = programWithWork)
        }
    }
}

@Composable
private fun WorkImage(imageUrl: String?, workTitle: String) {
    Box(
        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = workTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.surfaceVariant
                ),
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
}

@Composable
private fun WorkTags(programWithWork: ProgramWithWork) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            programWithWork.work.media?.let {
                InfoTag(text = it, color = MaterialTheme.colorScheme.primaryContainer)
            }
            programWithWork.work.seasonName?.let {
                InfoTag(text = it.name, color = MaterialTheme.colorScheme.secondaryContainer)
            }
            programWithWork.work.seasonYear?.let {
                InfoTag(
                    text = it.toString() + "年",
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            programWithWork.work.viewerStatusState.let {
                InfoTag(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
            InfoTag(
                text = programWithWork.firstProgram.channel.name,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeInfoRow(
    programWithWork: ProgramWithWork,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val episodeText = buildString {
                append(
                    programWithWork.firstProgram.episode.formattedNumber
                )
                programWithWork.firstProgram.episode.title?.let {
                    append(" ")
                    append(it)
                }
            }
            Text(
                text = episodeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = programWithWork.firstProgram.startedAt.format(
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RecordButton(
            episodeId = programWithWork.firstProgram.episode.id,
            workId = programWithWork.work.id,
            status = programWithWork.work.viewerStatusState,
            uiState = uiState,
            onRecordEpisode = onRecordEpisode
        )
    }
}

@Composable
private fun RecordButton(
    episodeId: String,
    workId: String,
    status: StatusState,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit
) {
    val isRecording = uiState.isRecording
    val recordingSuccess = uiState.recordingSuccess == episodeId
    FilledTonalIconButton(
        onClick = { onRecordEpisode(episodeId, workId, status) },
        modifier = Modifier.size(40.dp),
        enabled = !isRecording && !recordingSuccess,
        colors = if (recordingSuccess) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors()
        }
    ) {
        if (recordingSuccess) {
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

@Composable
fun InfoTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = contentColorFor(color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
