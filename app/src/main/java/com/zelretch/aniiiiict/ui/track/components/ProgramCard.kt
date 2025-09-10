package com.zelretch.aniiiiict.ui.track.components

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    uiState: TrackUiState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("program_card_${programWithWork.work.id}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = { onShowUnwatchedEpisodes(programWithWork) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkImage(
                imageUrl = programWithWork.work.image?.imageUrl,
                workTitle = programWithWork.work.title
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = programWithWork.work.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("work_title_${programWithWork.work.id}")
                )
                EpisodeInfo(programWithWork = programWithWork)
            }
            Spacer(modifier = Modifier.width(8.dp))
            RecordButton(
                episodeId = programWithWork.firstProgram.episode.id,
                workId = programWithWork.work.id,
                status = programWithWork.work.viewerStatusState,
                uiState = uiState,
                onRecordEpisode = onRecordEpisode
            )
        }
    }
}

@Composable
private fun WorkImage(imageUrl: String?, workTitle: String) {
    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 90.dp)
            .clip(RoundedCornerShape(8.dp))
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
}

@Composable
private fun EpisodeInfo(programWithWork: ProgramWithWork) {
    val episode = programWithWork.firstProgram.episode
    val episodeText = buildString {
        append(episode.numberText ?: "Ep ?")
        episode.title?.let {
            append(" - ")
            append(it)
        }
    }
    Text(
        text = episodeText,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = programWithWork.firstProgram.startedAt.format(
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
        modifier = Modifier.size(48.dp),
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
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "記録する",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
