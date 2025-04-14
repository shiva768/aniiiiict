package com.zelretch.aniiiiiict.ui.unwatched.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Program
import java.time.format.DateTimeFormatter

@Composable
fun EpisodeCard(
    program: Program,
    onRecordEpisode: (String) -> Unit,
    onMarkUpToAsWatched: () -> Unit,
    onDelete: () -> Unit
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
                text = "第${program.episode.number}話" + (program.episode.title?.let { " $it" }
                    ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = program.startedAt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onMarkUpToAsWatched) {
                    Text("ここまでまとめて視聴済みにする")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onRecordEpisode(program.episode.id)
                        onDelete()
                    }
                ) {
                    Text("視聴済みにする")
                }
            }
        }
    }
} 