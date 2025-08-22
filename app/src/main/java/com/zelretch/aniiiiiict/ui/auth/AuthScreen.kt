package com.zelretch.aniiiiiict.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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

private const val LOGIN_BUTTON_WIDTH_RATIO = 0.7f
private val PADDING_MEDIUM = 16.dp
private val LOGO_SIZE = 120.dp
private val SPACER_HEIGHT_LARGE = 24.dp
private val SPACER_HEIGHT_XLARGE = 32.dp
private val LOGIN_BUTTON_HEIGHT = 48.dp

@Composable
fun AuthScreen(uiState: MainUiState, onLoginClick: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in the snackbar if present
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(PADDING_MEDIUM),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App logo or icon
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(LOGO_SIZE)
                )

                Spacer(modifier = Modifier.height(SPACER_HEIGHT_LARGE))

                // App name
                Text(
                    text = "Aniiiiiict",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(PADDING_MEDIUM))

                // Description
                Text(
                    text = "Annictと連携して、アニメの視聴記録を管理しましょう。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SPACER_HEIGHT_XLARGE))

                // Login button
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(LOGIN_BUTTON_WIDTH_RATIO).height(LOGIN_BUTTON_HEIGHT),
                    enabled = !uiState.isAuthenticating && !uiState.isLoading
                ) {
                    Text("Annictでログイン")
                }

                // Show loading indicator if authenticating
                if (uiState.isAuthenticating || uiState.isLoading) {
                    Spacer(modifier = Modifier.height(SPACER_HEIGHT_LARGE))
                    CircularProgressIndicator()
                }
            }
        }
    }
}
