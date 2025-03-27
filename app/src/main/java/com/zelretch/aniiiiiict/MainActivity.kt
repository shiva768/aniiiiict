package com.zelretch.aniiiiiict

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.zelretch.aniiiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiiict.ui.main.MainScreen
import com.zelretch.aniiiiiict.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

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
                    MainScreen(viewModel = viewModel)
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
                if (code != null) {
                    viewModel.handleAuthCallback(code)
                }
            }
        }
    }
} 