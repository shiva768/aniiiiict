package com.zelretch.aniiiiiict.ui.unwatched

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.type.StatusState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnwatchedEpisodesModal(
    programWithWork: ProgramWithWork,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onBulkRecordEpisode: (List<String>, String, StatusState) -> Unit
) {
    // デバッグ情報
    val episodeCount = programWithWork.programs.size
    programWithWork.work.title
    val programs =
        remember { mutableStateListOf<Program>().apply { addAll(programWithWork.programs) } }

    // 確認ダイアログの状態
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedEpisodeIndex by remember { mutableStateOf<Int?>(null) }

    // 確認ダイアログ
    if (showConfirmDialog && selectedEpisodeIndex != null) {
        val episodeIndex = selectedEpisodeIndex!!

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedEpisodeIndex = null
            },
            title = { Text("ここまでまとめて視聴済みにする") },
            text = { Text("第${programs[episodeIndex].episode.number}話まで、合計${episodeCount}話を視聴済みにします。\nこの操作は取り消せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetEpisodes =
                            programs.filterIndexed { index, _ -> index <= episodeIndex }
                        val episodeIds = targetEpisodes.map { it.episode.id }
                        onBulkRecordEpisode(
                            episodeIds,
                            programWithWork.work.id,
                            StatusState.WATCHED
                        )
                        programs.removeAll(targetEpisodes)
                        showConfirmDialog = false
                        selectedEpisodeIndex = null
                    }
                ) {
                    Text("視聴済みにする")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedEpisodeIndex = null
                    }
                ) {
                    Text("キャンセル")
                }
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
                Text("未視聴エピソード (${programs.size}件)")
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "閉じる"
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (programs.isEmpty()) {
                    Text(
                        text = "未視聴のエピソードはありません",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(programs) { program ->
                            EpisodeCard(
                                episode = program.episode,
                                programWithWork = programWithWork,
                                onRecordEpisode = onRecordEpisode,
                                onMarkUpToAsWatched = { index ->
                                    selectedEpisodeIndex = index
                                    showConfirmDialog = true
                                },
                                onDelete = { programs.remove(program) },
                                index = programs.indexOf(program)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeCard(
    episode: Episode,
    programWithWork: ProgramWithWork,
    onRecordEpisode: (String, String, StatusState) -> Unit,
    onMarkUpToAsWatched: (Int) -> Unit,
    onDelete: () -> Unit,
    index: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "第${episode.number}話" + (episode.title?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onMarkUpToAsWatched(index) }
                ) {
                    Text("ここまでまとめて視聴済みにする")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onRecordEpisode(episode.id, programWithWork.work.id, StatusState.WATCHED)
                        onDelete()
                    }
                ) {
                    Text("視聴済みにする")
                }
            }
        }
    }
}