package com.zelretch.aniiiiict.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.ui.common.components.StatusDropdown
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WatchingEpisodeModal(
    entry: LibraryEntry,
    onDismiss: () -> Unit,
    viewModel: WatchingEpisodeModalViewModel = hiltViewModel<WatchingEpisodeModalViewModel>(),
    onRefresh: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    WatchingEpisodeModalLaunchedEffects(viewModel, entry, onRefresh, onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            WatchingEpisodeModalTitle(
                state = state,
                onDismiss = onDismiss,
                onStatusChange = viewModel::changeStatus
            )
        },
        text = {
            WatchingEpisodeModalContent(
                state = state,
                onRecordEpisode = viewModel::recordEpisode
            )
        },
        confirmButton = { }
    )
}

@Composable
private fun WatchingEpisodeModalLaunchedEffects(
    viewModel: WatchingEpisodeModalViewModel,
    entry: LibraryEntry,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(entry) {
        viewModel.initialize(entry)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is WatchingEpisodeModalEvent.StatusChanged -> onRefresh()
                is WatchingEpisodeModalEvent.EpisodeRecorded -> {
                    onRefresh()
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun WatchingEpisodeModalTitle(
    state: WatchingEpisodeModalState,
    onDismiss: () -> Unit,
    onStatusChange: (StatusState) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.workTitle,
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

@Composable
private fun WatchingEpisodeModalContent(
    state: WatchingEpisodeModalState,
    onRecordEpisode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.episode?.let { episode ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "次のエピソード",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = episode.formattedNumber,
                    style = MaterialTheme.typography.titleMedium
                )
                episode.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRecordEpisode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("視聴済みにする")
            }
        } ?: run {
            Text(
                text = "次のエピソード情報がありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
