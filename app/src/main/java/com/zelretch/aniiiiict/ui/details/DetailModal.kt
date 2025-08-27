package com.zelretch.aniiiiict.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.details.components.ConfirmDialog
import com.zelretch.aniiiiict.ui.details.components.UnwatchedEpisodesContent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailModal(
    programWithWork: ProgramWithWork,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    viewModel: DetailModalViewModel = hiltViewModel(),
    onRefresh: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    DetailModalLaunchedEffects(viewModel, programWithWork, onRefresh)

    DetailModalDialogs(viewModel, state, programWithWork)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DetailModalTitle(state = state, onDismiss = onDismiss, onStatusChange = viewModel::changeStatus) },
        text = {
            UnwatchedEpisodesContent(programs = state.programs, isLoading = isLoading, onRecordEpisode = { episodeId ->
                viewModel.recordEpisode(episodeId, programWithWork.work.viewerStatusState)
            }, onMarkUpToAsWatched = { index ->
                viewModel.showConfirmDialog(index)
            })
        },
        confirmButton = { }
    )
}

@Composable
private fun DetailModalLaunchedEffects(
    viewModel: DetailModalViewModel,
    programWithWork: ProgramWithWork,
    onRefresh: () -> Unit
) {
    LaunchedEffect(programWithWork) {
        viewModel.initialize(programWithWork)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DetailModalEvent.StatusChanged,
                is DetailModalEvent.EpisodesRecorded,
                is DetailModalEvent.BulkEpisodesRecorded
                -> onRefresh()
            }
        }
    }
}

@Composable
private fun DetailModalDialogs(
    viewModel: DetailModalViewModel,
    state: DetailModalState,
    programWithWork: ProgramWithWork
) {
    if (state.showConfirmDialog && state.selectedEpisodeIndex != null) {
        val episodeIndex = state.selectedEpisodeIndex
        ConfirmDialog(
            episodeNumber = state.programs[episodeIndex].episode.number ?: 0,
            episodeCount = episodeIndex + 1,
            onConfirm = {
                val targetEpisodes = state.programs.slice(0..episodeIndex)
                val episodeIds = targetEpisodes.map { it.episode.id }
                viewModel.bulkRecordEpisodes(episodeIds, programWithWork.work.viewerStatusState)
            },
            onDismiss = viewModel::hideConfirmDialog
        )
    }

    if (state.isBulkRecording) {
        AlertDialog(onDismissRequest = { }, title = { Text("エピソードを記録中...") }, text = {
            Column {
                Text("${state.bulkRecordingProgress}/${state.bulkRecordingTotal}件のエピソードを記録中")
                LinearProgressIndicator(
                    progress = {
                        if (state.bulkRecordingTotal > 0) {
                            state.bulkRecordingProgress.toFloat() / state.bulkRecordingTotal.toFloat()
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }, confirmButton = { })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailModalTitle(state: DetailModalState, onDismiss: () -> Unit, onStatusChange: (StatusState) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "未視聴エピソード (${state.programs.size}件)",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }

        StatusDropdownMenu(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            state = state,
            onStatusChange = onStatusChange
        )

        state.statusChangeError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StatusDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    state: DetailModalState,
    onStatusChange: (StatusState) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(expanded = expanded && !state.isStatusChanging, onExpandedChange = {
            onExpandedChange(!expanded)
        }) {
            TextField(
                value = state.selectedStatus?.name ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = !state.isStatusChanging,
                trailingIcon = {
                    if (state.isStatusChanging) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(expanded = expanded && !state.isStatusChanging, onDismissRequest = {
                onExpandedChange(false)
            }) {
                StatusState.entries.forEach { status ->
                    DropdownMenuItem(text = { Text(status.name) }, onClick = {
                        onExpandedChange(false)
                        onStatusChange(status)
                    })
                }
            }
        }
    }
}
