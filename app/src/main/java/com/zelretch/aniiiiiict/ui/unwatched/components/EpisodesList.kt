package com.zelretch.aniiiiiict.ui.unwatched.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.data.model.Program

@Composable
fun EpisodesList(
    programs: List<Program>,
    onRecordEpisode: (String) -> Unit,
    onMarkUpToAsWatched: (Int) -> Unit,
    onDeleteEpisode: (Program) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(programs) { program ->
            EpisodeCard(
                program = program,
                onRecordEpisode = { onRecordEpisode(it) },
                onMarkUpToAsWatched = { onMarkUpToAsWatched(programs.indexOf(program)) },
                onDelete = { onDeleteEpisode(program) }
            )
        }
    }
} 