package com.zelretch.aniiiiiict.ui.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Program

private val CARD_CORNER_RADIUS = 16.dp
private val CARD_ELEVATION = 2.dp
private val VERTICAL_SPACING = 3.dp
private val HORIZONTAL_SPACING = 4.dp
private val BUTTON_CORNER_RADIUS = 10.dp
private val BUTTON_ICON_SIZE = 16.dp
private const val SECOND_BUTTON_WEIGHT = 1.2f

@Composable
fun EpisodeCard(program: Program, onRecordEpisode: (String) -> Unit, onMarkUpToAsWatched: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = CARD_ELEVATION)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CARD_CORNER_RADIUS),
            verticalArrangement = Arrangement.spacedBy(VERTICAL_SPACING)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VERTICAL_SPACING)
            ) {
                Text(
                    text = "第${program.episode.number}話",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                program.episode.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HORIZONTAL_SPACING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        onRecordEpisode(program.episode.id)
                    },
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(BUTTON_ICON_SIZE)
                    )
                    Text(
                        text = "記録する",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                FilledTonalButton(
                    onClick = onMarkUpToAsWatched,
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                    modifier = Modifier.weight(SECOND_BUTTON_WEIGHT)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = null,
                        modifier = Modifier.size(BUTTON_ICON_SIZE)
                    )
                    Text(
                        text = "ここまで記録",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
