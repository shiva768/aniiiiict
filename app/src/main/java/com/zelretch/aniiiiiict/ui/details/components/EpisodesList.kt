package com.zelretch.aniiiiiict.ui.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Program

@Composable
fun EpisodesList(programs: List<Program>, onRecordEpisode: (String) -> Unit, onMarkUpToAsWatched: (Int) -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = programs, key = { it.id }) { program ->
                EpisodeCard(
                    program = program,
                    onRecordEpisode = onRecordEpisode,
                    onMarkUpToAsWatched = { onMarkUpToAsWatched(programs.indexOf(program)) }
                )
            }
        }
    }
}
