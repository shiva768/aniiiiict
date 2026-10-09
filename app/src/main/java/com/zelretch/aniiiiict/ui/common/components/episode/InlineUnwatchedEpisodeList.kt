package com.zelretch.aniiiiict.ui.common.components.episode

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiict.data.model.Episode

private const val DISABLED_ROW_ALPHA = 0.4f

/**
 * 未視聴エピソードのインライン一覧。タップした話までをまとめて記録する。
 * 放送スケジュール（Track）とライブラリの「まとめて」で共通利用する。
 */
@Composable
fun InlineUnwatchedEpisodeList(
    episodes: List<Episode>,
    isRecording: Boolean,
    onRecordUpTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = "未視聴 ${episodes.size}話 ・ タップで記録",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            episodes.forEachIndexed { index, episode ->
                InlineUnwatchedRow(
                    episode = episode,
                    index = index,
                    filled = pressedIndex != null && index <= pressedIndex!!,
                    showCountChip = pressedIndex == index,
                    enabled = !isRecording,
                    onPressedChange = { pressed -> pressedIndex = if (pressed) index else null },
                    onClick = { onRecordUpTo(index) }
                )
            }
        }
    }
}

@Composable
private fun InlineUnwatchedRow(
    episode: Episode,
    index: Int,
    filled: Boolean,
    showCountChip: Boolean,
    enabled: Boolean,
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
            // 記録処理中はタップを無効化し、二度押しによる重複記録を防ぐ
            .alpha(if (enabled) 1f else DISABLED_ROW_ALPHA)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
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
            append(episode.formattedNumber)
            episode.title?.let { append("「$it」") }
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
