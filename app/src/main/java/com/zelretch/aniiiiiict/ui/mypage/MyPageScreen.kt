package com.zelretch.aniiiiiict.ui.mypage

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Text(text = "Loading...")
    } else if (uiState.error != null) {
        Text(text = "Error: ${uiState.error}")
    } else {
        Text(text = "Data loaded. Chart implementation pending.")
        // TODO: Implement charts here
    }
}
