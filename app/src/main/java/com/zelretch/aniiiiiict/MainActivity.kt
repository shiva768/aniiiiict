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
import com.zelretch.aniiiiiict.ui.mypage.MyPageScreen
import com.zelretch.aniiiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiiict.ui.track.TrackScreen
import com.zelretch.aniiiiiict.ui.track.TrackViewModel
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    @Inject
    lateinit var logger: Logger

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication state when activity is created
        mainViewModel.checkAuthentication()

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
                            onNavigateToMyPage = { navController.navigate("mypage") },
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

                    composable("mypage") {
                        MyPageScreen()
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
        logger.debug(
            TAG,
            "Intent received: ${intent.action}, data: ${intent.data}",
            "handleIntent"
        )
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                logger.debug(TAG, "Received OAuth callback: $uri", "handleIntent")
                // Extract the auth code from the URI
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    logger.debug(
                        TAG,
                        "Processing authentication code: ${code.take(5)}...",
                        "handleIntent"
                    )
                    logger.info(
                        TAG,
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
                    logger.error(
                        TAG,
                        "認証コードがURIに含まれていません: $uri",
                        "handleIntent"
                    )
                }
            }
        }
    }
}
