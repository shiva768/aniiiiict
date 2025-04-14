package com.zelretch.aniiiiiict.ui.unwatched.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Program

@Composable
fun UnwatchedEpisodesContent(
    programs: List<Program>,
    isLoading: Boolean,
    onRecordEpisode: (String) -> Unit,
    onMarkUpToAsWatched: (Int) -> Unit,
    onDeleteEpisode: (Program) -> Unit
) {
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
            EpisodesList(
                programs = programs,
                onRecordEpisode = onRecordEpisode,
                onMarkUpToAsWatched = onMarkUpToAsWatched,
                onDeleteEpisode = onDeleteEpisode
            )
        }
    }
} 