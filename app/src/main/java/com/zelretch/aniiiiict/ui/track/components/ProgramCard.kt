package com.zelretch.aniiiiict.ui.track.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.common.components.toJapaneseLabel
import com.zelretch.aniiiiict.ui.track.TrackUiState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordUpTo: (Int) -> Unit,
    onShowAnimeDetail: (ProgramWithWork) -> Unit,
    uiState: TrackUiState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("program_card_${programWithWork.work.id}")
            .clickable { onShowAnimeDetail(programWithWork) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            WorkInfoSection(programWithWork = programWithWork)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            EpisodeInfoSection(
                programWithWork = programWithWork,
                uiState = uiState,
                onRecordEpisode = onRecordEpisode,
                onBulkRecordUpTo = onBulkRecordUpTo
            )
        }
    }
}

@Composable
private fun WorkInfoSection(programWithWork: ProgramWithWork) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkImage(
            imageUrl = programWithWork.work.image?.imageUrl,
            workTitle = programWithWork.work.title
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = programWithWork.work.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("work_title_${programWithWork.work.id}")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoTag(
                    text = programWithWork.firstProgram.channel.name,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                InfoTag(
                    text = programWithWork.work.viewerStatusState.toJapaneseLabel(),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
            val seasonMeta = buildSeasonMeta(programWithWork)
            if (seasonMeta.isNotEmpty()) {
                Text(
                    text = seasonMeta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkImage(imageUrl: String?, workTitle: String) {
    Box(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun EpisodeInfoSection(
    programWithWork: ProgramWithWork,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordUpTo: (Int) -> Unit
) {
    var expanded by remember(programWithWork.work.id) { mutableStateOf(false) }
    val unwatchedCount = programWithWork.programs.size
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val episodeText = buildString {
                append(programWithWork.firstProgram.episode.formattedNumber)
                programWithWork.firstProgram.episode.title?.let {
                    append("「$it」")
                }
            }
            Text(
                text = episodeText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = programWithWork.firstProgram.startedAt.format(
                    DateTimeFormatter.ofPattern("M月d日(E) HH:mm", Locale.JAPANESE)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordButton(
                episodeId = programWithWork.firstProgram.episode.id,
                workId = programWithWork.work.id,
                status = programWithWork.work.viewerStatusState,
                uiState = uiState,
                onRecordEpisode = onRecordEpisode,
                modifier = Modifier.weight(1f)
            )
            BulkRecordButton(
                unwatchedCount = unwatchedCount,
                enabled = !uiState.isRecording,
                onClick = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            InlineUnwatchedList(
                programWithWork = programWithWork,
                onBulkRecordUpTo = onBulkRecordUpTo
            )
        }
    }
}

@Composable
private fun BulkRecordButton(
    unwatchedCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.testTag("bulk_record_button"),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "まとめて",
            style = MaterialTheme.typography.labelMedium
        )
        if (unwatchedCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = unwatchedCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun InlineUnwatchedList(programWithWork: ProgramWithWork, onBulkRecordUpTo: (Int) -> Unit) {
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = "未視聴 ${programWithWork.programs.size}話 ・ タップで記録",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            programWithWork.programs.forEachIndexed { index, program ->
                InlineUnwatchedRow(
                    program = program,
                    index = index,
                    filled = pressedIndex != null && index <= pressedIndex!!,
                    showCountChip = pressedIndex == index,
                    onPressedChange = { pressed -> pressedIndex = if (pressed) index else null },
                    onClick = { onBulkRecordUpTo(index) }
                )
            }
        }
    }
}

@Composable
private fun InlineUnwatchedRow(
    program: com.zelretch.aniiiiict.data.model.Program,
    index: Int,
    filled: Boolean,
    showCountChip: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(isPressed) { onPressedChange(isPressed) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inline_episode_$index")
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (filled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        val episodeText = buildString {
            append(program.episode.formattedNumber)
            program.episode.title?.let { append("「$it」") }
        }
        Text(
            text = episodeText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showCountChip) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${index + 1}話",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordButton(
    episodeId: String,
    workId: String,
    status: StatusState,
    uiState: TrackUiState,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = uiState.isRecording
    val recordingSuccess = uiState.recordingSuccess == episodeId
    Button(
        onClick = { onRecordEpisode(episodeId, workId, status) },
        modifier = modifier,
        enabled = !isRecording && !recordingSuccess,
        shape = RoundedCornerShape(10.dp),
        colors = if (recordingSuccess) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Icon(
            imageVector = if (recordingSuccess) Icons.Default.Check else Icons.Default.CheckCircle,
            contentDescription = if (recordingSuccess) "記録済み" else "この話を記録",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (recordingSuccess) "記録済み" else "この話を記録",
            style = MaterialTheme.typography.labelMedium
        )
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

private fun buildSeasonMeta(programWithWork: ProgramWithWork): String = buildString {
    programWithWork.work.seasonYear?.let { append("${it}年") }
    programWithWork.work.seasonName?.let { append(it.name) }
    programWithWork.work.media?.let {
        if (isNotEmpty()) append(" · ")
        append(it)
    }
}
