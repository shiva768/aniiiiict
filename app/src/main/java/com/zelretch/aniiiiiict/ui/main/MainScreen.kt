package com.zelretch.aniiiiiict.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.ui.components.DatePickerDialog
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onProgramClick: (ProgramWithWork) -> Unit,
    onDateChange: (LocalDateTime) -> Unit,
    onImageLoad: (Int, String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Aniiiiict") }, actions = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select date")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.programs) { program ->
                        ProgramCard(
                            programWithWork = program,
                            onClick = { onProgramClick(program) },
                            onImageLoad = {
                                program.work.image?.recommendedImageUrl?.let { imageUrl ->
                                    onImageLoad(program.program.annictId, imageUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(onDismiss = { showDatePicker = false }, onDateSelected = { date ->
            onDateChange(date.atTime(LocalTime.now()))
            showDatePicker = false
        }, onError = {
            // エラー処理は省略
            showDatePicker = false
        })
    }
}

@Composable
fun ProgramCard(
    programWithWork: ProgramWithWork,
    onClick: () -> Unit,
    onImageLoad: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = programWithWork.work.image?.recommendedImageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = programWithWork.work.title,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { onImageLoad() }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = programWithWork.work.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = programWithWork.work.media ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Episode ${programWithWork.program.episode.numberText ?: "?"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = programWithWork.program.startedAt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 