package com.zelretch.aniiiiiict.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zelretch.aniiiiiict.MainUiState
import com.zelretch.aniiiiiict.R

@Composable
fun AuthScreen(
    uiState: MainUiState,
    onLoginClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar if present
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App logo or icon
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App name
                Text(
                    text = "Aniiiiiict",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Annictと連携して、アニメの視聴記録を管理しましょう。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Login button
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    enabled = !uiState.isAuthenticating && !uiState.isLoading
                ) {
                    Text("Annictでログイン")
                }

                // Show loading indicator if authenticating
                if (uiState.isAuthenticating || uiState.isLoading) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
}