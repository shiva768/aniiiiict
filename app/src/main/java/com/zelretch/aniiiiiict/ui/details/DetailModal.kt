package com.zelretch.aniiiiiict.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.ui.details.components.ConfirmDialog
import com.zelretch.aniiiiiict.ui.details.components.UnwatchedEpisodesContent

@Composable
fun DetailModal(
    programWithWork: ProgramWithWork,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordEpisode: (List<String>, String, StatusState) -> Unit
) {
    val programs =
        remember { mutableStateListOf<Program>().apply { addAll(programWithWork.programs) } }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedEpisodeIndex by remember { mutableStateOf<Int?>(null) }

    if (showConfirmDialog && selectedEpisodeIndex != null) {
        val episodeIndex = selectedEpisodeIndex!!
        ConfirmDialog(
            episodeNumber = programs[episodeIndex].episode.number ?: 0,
            episodeCount = episodeIndex + 1,
            onConfirm = {
                val targetEpisodes = programs.filterIndexed { index, _ -> index <= episodeIndex }
                val episodeIds = targetEpisodes.map { it.episode.id }
                onBulkRecordEpisode(episodeIds, programWithWork.work.id, programWithWork.work.viewerStatusState)
                programs.removeAll(targetEpisodes)
                showConfirmDialog = false
                selectedEpisodeIndex = null
            },
            onDismiss = {
                showConfirmDialog = false
                selectedEpisodeIndex = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "未視聴エピソード (${programs.size}件)",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "閉じる"
                    )
                }
            }
        },
        text = {
            UnwatchedEpisodesContent(
                programs = programs,
                isLoading = isLoading,
                onRecordEpisode = { episodeId ->
                    onRecordEpisode(episodeId, programWithWork.work.id, programWithWork.work.viewerStatusState)
                },
                onMarkUpToAsWatched = { index ->
                    selectedEpisodeIndex = index
                    showConfirmDialog = true
                },
                onDeleteEpisode = { program ->
                    programs.remove(program)
                }
            )
        },
        confirmButton = { }
    )
}