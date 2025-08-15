package com.zelretch.aniiiiiict.ui.details

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
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.ui.details.components.ConfirmDialog
import com.zelretch.aniiiiiict.ui.details.components.UnwatchedEpisodesContent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailModal(
    programWithWork: ProgramWithWork,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    viewModel: DetailModalViewModel = hiltViewModel(),
    onRefresh: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // ViewModelの初期化
    LaunchedEffect(programWithWork) {
        viewModel.initialize(programWithWork)
    }

    // イベントの監視
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DetailModalEvent.StatusChanged -> onRefresh()
                is DetailModalEvent.EpisodesRecorded -> onRefresh()
                is DetailModalEvent.BulkEpisodesRecorded -> onRefresh()
            }
        }
    }

    if (state.showConfirmDialog && state.selectedEpisodeIndex != null) {
        val episodeIndex = state.selectedEpisodeIndex!!
        ConfirmDialog(
            episodeNumber = state.programs[episodeIndex].episode.number ?: 0,
            episodeCount = episodeIndex + 1,
            onConfirm = {
                val targetEpisodes = state.programs.filterIndexed { index, _ -> index <= episodeIndex }
                val episodeIds = targetEpisodes.map { it.episode.id }
                viewModel.bulkRecordEpisodes(episodeIds, programWithWork.work.viewerStatusState)
            },
            onDismiss = {
                viewModel.hideConfirmDialog()
            },
        )
    }

    if (state.isBulkRecording) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("エピソードを記録中...") },
            text = {
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "未視聴エピソード (${state.programs.size}件)",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded && !state.isStatusChanging,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        TextField(
                            value = state.selectedStatus?.toString() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = !state.isStatusChanging,
                            trailingIcon = {
                                if (state.isStatusChanging) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(8.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                }
                            },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded && !state.isStatusChanging,
                            onDismissRequest = { expanded = false },
                        ) {
                            StatusState.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.toString()) },
                                    onClick = {
                                        expanded = false
                                        viewModel.changeStatus(status)
                                    },
                                )
                            }
                        }
                    }
                }

                state.statusChangeError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        text = {
            UnwatchedEpisodesContent(
                programs = state.programs,
                isLoading = isLoading,
                onRecordEpisode = { episodeId ->
                    viewModel.recordEpisode(episodeId, programWithWork.work.viewerStatusState)
                },
                onMarkUpToAsWatched = { index ->
                    viewModel.showConfirmDialog(index)
                },
            )
        },
        confirmButton = { },
    )
}
