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
import com.zelretch.aniiiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiiict.ui.history.HistoryViewModel
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

                NavHost(
                    navController = navController,
                    startDestination = "track"
                ) {
                    composable("track") {
                        val trackViewModel: TrackViewModel = hiltViewModel()
                        val trackUiState by trackViewModel.uiState.collectAsState()
                        TrackScreen(
                            viewModel = trackViewModel,
                            uiState = trackUiState,
                            onRecordEpisode = { id, workId, status ->
                                trackViewModel.recordEpisode(id, workId, status)
                            },
                            onBulkRecordEpisode = { ids, workId, status ->
                                trackViewModel.bulkRecordEpisode(ids, workId, status)
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