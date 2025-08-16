package com.zelretch.aniiiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiiict.ui.auth.AuthScreen
import com.zelretch.aniiiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiiict.ui.history.HistoryViewModel
import com.zelretch.aniiiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiiict.ui.track.TrackScreen
import com.zelretch.aniiiiiict.ui.track.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication state when activity is created
        mainViewModel.checkAuthentication()

        // サンプルコードではWindowアニメーションが無効化されていないケースが多いため
        // ここで明示的に無効化する
        (window.decorView.context as android.content.Context).theme?.obtainStyledAttributes(
            intArrayOf(android.R.attr.windowAnimationStyle)
        )?.let {
            try {
                val windowAnimationStyleResId = it.getResourceId(0, 0)
                if (windowAnimationStyleResId != 0) {
                    val windowAnimationStyle = resources.newTheme()
                    windowAnimationStyle.applyStyle(windowAnimationStyleResId, false)
                    windowAnimationStyle.obtainStyledAttributes(
                        intArrayOf(
                            android.R.attr.activityOpenEnterAnimation,
                            android.R.attr.activityOpenExitAnimation,
                            android.R.attr.activityCloseEnterAnimation,
                            android.R.attr.activityCloseExitAnimation
                        )
                    ).let { animAttrs ->
                        try {
                            // アニメーションの期間を0に設定して実質的に無効化
                            window.setWindowAnimations(0)
                        } finally {
                            animAttrs.recycle()
                        }
                    }
                }
            } finally {
                it.recycle()
            }
        }

        setContent {
            AniiiiictTheme {
                val navController = rememberNavController()
                val mainUiState by mainViewModel.uiState.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = "auth"
                ) {
                    composable("auth") {
                        // Use LaunchedEffect to handle navigation based on authentication state
                        androidx.compose.runtime.LaunchedEffect(mainUiState.isAuthenticated) {
                            if (mainUiState.isAuthenticated) {
                                // Navigate to track screen if authenticated
                                navController.navigate("track") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        }

                        AuthScreen(uiState = mainUiState, onLoginClick = {
                            mainViewModel.startAuth()
                        })
                    }

                    composable("track") {
                        val trackViewModel: TrackViewModel = hiltViewModel()
                        val trackUiState by trackViewModel.uiState.collectAsState()
                        TrackScreen(
                            viewModel = trackViewModel,
                            uiState = trackUiState,
                            onRecordEpisode = { id, workId, status ->
                                trackViewModel.recordEpisode(id, workId, status)
                            },
                            onNavigateToHistory = { navController.navigate("history") },
                            onRefresh = { trackViewModel.refresh() }
                        )
                    }

                    composable("history") {
                        val historyViewModel: HistoryViewModel = hiltViewModel()
                        val historyUiState by historyViewModel.uiState.collectAsState()

                        HistoryScreen(
                            uiState = historyUiState,
                            onNavigateBack = { navController.navigateUp() },
                            onRetry = { historyViewModel.loadRecords() },
                            onDeleteRecord = { historyViewModel.deleteRecord(it) },
                            onRefresh = { historyViewModel.loadRecords() },
                            onLoadNextPage = { historyViewModel.loadNextPage() },
                            onSearchQueryChange = { historyViewModel.updateSearchQuery(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Timber.d(
            "MainActivity",
            "Intent received: ${intent.action}, data: ${intent.data}",
            "handleIntent"
        )
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                Timber.d("MainActivity", "Received OAuth callback: $uri", "handleIntent")
                // Extract the auth code from the URI
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    Timber.d(
                        "MainActivity",
                        "Processing authentication code: ${code.take(5)}...",
                        "handleIntent"
                    )
                    Timber.i(
                        "MainActivity",
                        "Processing authentication code: ${code.take(5)}...",
                        "handleIntent"
                    )

                    // 認証処理は新しいコルーチンで実行して独立性を保つ
                    lifecycleScope.launch {
                        mainViewModel.handleAuthCallback(code)
                        // 認証状態を再確認して UI を更新
                        mainViewModel.checkAuthentication()
                    }
                } else {
                    Timber.e(
                        "MainActivity",
                        "認証コードがURIに含まれていません: $uri",
                        "handleIntent"
                    )
                }
            }
        }
    }
}
