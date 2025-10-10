package com.zelretch.aniiiiict.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.ui.common.components.StatusDropdown
import com.zelretch.aniiiiict.ui.details.components.ConfirmDialog
import com.zelretch.aniiiiict.ui.details.components.FinaleConfirmDialog
import com.zelretch.aniiiiict.ui.details.components.UnwatchedEpisodesContent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BroadcastEpisodeModal(
    programWithWork: ProgramWithWork,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    viewModel: BroadcastEpisodeModalViewModel = hiltViewModel<BroadcastEpisodeModalViewModel>(),
    onRefresh: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    BroadcastEpisodeModalLaunchedEffects(viewModel, programWithWork, onRefresh)

    BroadcastEpisodeModalDialogs(viewModel, state, programWithWork)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { BroadcastEpisodeModalTitle(state = state, onDismiss = onDismiss, onStatusChange = viewModel::changeStatus) },
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
private fun BroadcastEpisodeModalLaunchedEffects(
    viewModel: BroadcastEpisodeModalViewModel,
    programWithWork: ProgramWithWork,
    onRefresh: () -> Unit
) {
    LaunchedEffect(programWithWork) {
        viewModel.initialize(programWithWork)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BroadcastEpisodeModalEvent.StatusChanged,
                is BroadcastEpisodeModalEvent.EpisodesRecorded,
                is BroadcastEpisodeModalEvent.BulkEpisodesRecorded
                -> onRefresh()
                is BroadcastEpisodeModalEvent.FinaleConfirmationShown
                -> { /* UI already handles this via state */ }
            }
        }
    }
}

@Composable
private fun BroadcastEpisodeModalDialogs(
    viewModel: BroadcastEpisodeModalViewModel,
    state: BroadcastEpisodeModalState,
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
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "エピソードを記録中...",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "${state.bulkRecordingProgress}/${state.bulkRecordingTotal}件のエピソードを記録中",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (state.bulkRecordingTotal > 0) {
                                state.bulkRecordingProgress.toFloat() / state.bulkRecordingTotal.toFloat()
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
            },
            confirmButton = { }
        )
    }

    if (state.showFinaleConfirmation && state.finaleEpisodeNumber != null) {
        FinaleConfirmDialog(
            episodeNumber = state.finaleEpisodeNumber,
            onConfirm = viewModel::confirmFinaleWatched,
            onDismiss = viewModel::hideFinaleConfirmation
        )
    }

    if (state.showSingleEpisodeFinaleConfirmation && state.singleEpisodeFinaleNumber != null) {
        FinaleConfirmDialog(
            episodeNumber = state.singleEpisodeFinaleNumber,
            onConfirm = viewModel::confirmSingleEpisodeFinaleWatched,
            onDismiss = viewModel::hideSingleEpisodeFinaleConfirmation
        )
    }
}

@Composable
private fun BroadcastEpisodeModalTitle(state: BroadcastEpisodeModalState, onDismiss: () -> Unit, onStatusChange: (StatusState) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "未視聴エピソード (${state.programs.size}件)",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }

        StatusDropdown(
            selectedStatus = state.selectedStatus,
            isChanging = state.isStatusChanging,
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
