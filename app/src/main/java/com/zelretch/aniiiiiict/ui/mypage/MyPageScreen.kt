package com.zelretch.aniiiiiict.ui.mypage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Main content
            Column {
                Text("My Page / Stats")
                Button(onClick = onNavigateToHistory) {
                    Text("Go to Full History")
                }
                // Placeholder for the contribution graph
                LazyColumn {
                    uiState.activitiesByMonth.forEach { (year, months) ->
                        item { Text(text = "Year: $year") }
                        months.forEach { (month, activities) ->
                            item { Text(text = "  Month: $month (${activities.size} records)") }
                            // Optionally list activities
                        }
                    }
                }
            }
        }
    }
}
