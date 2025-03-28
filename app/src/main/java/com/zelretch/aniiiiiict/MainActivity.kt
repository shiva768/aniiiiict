package com.zelretch.aniiiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.zelretch.aniiiiiict.ui.main.MainScreen
import com.zelretch.aniiiiiict.ui.main.MainViewModel
import com.zelretch.aniiiiiict.ui.theme.AniiiiictTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 認証コールバックの処理
        handleIntent(intent)

        setContent {
            AniiiiictTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    viewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    MainScreen(
                        uiState = uiState,
                        onProgramClick = viewModel::onProgramClick,
                        onDateChange = viewModel::onDateChange,
                        onImageLoad = viewModel::onImageLoad
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri?.scheme == "aniiiiiict" && uri.host == "oauth" && uri.pathSegments.firstOrNull() == "callback") {
                val code = uri.getQueryParameter("code")
                if (code != null && ::viewModel.isInitialized) {
                    viewModel.handleAuthCallback(code)
                }
            }
        }
    }
} 