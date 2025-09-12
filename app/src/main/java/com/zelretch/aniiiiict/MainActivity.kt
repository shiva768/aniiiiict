package com.zelretch.aniiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiict.ui.auth.AuthScreen
import com.zelretch.aniiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiict.ui.history.HistoryScreenActions
import com.zelretch.aniiiiict.ui.history.HistoryViewModel
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiict.ui.track.TrackScreen
import com.zelretch.aniiiiict.ui.track.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val DEFAULT_RESOURCE_ID = 0
        private const val AUTH_CODE_LOG_LENGTH = 5
    }

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.checkAuthentication()
        disableWindowAnimations()
        setContent {
            AniiiiictTheme {
                AppNavigation(mainViewModel)
            }
        }
    }

    private fun disableWindowAnimations() {
        val theme = (window.decorView.context as android.content.Context).theme ?: return
        val styledAttributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowAnimationStyle))
        try {
            applyWindowAnimations(styledAttributes)
        } finally {
            styledAttributes.recycle()
        }
    }

    private fun applyWindowAnimations(styledAttributes: android.content.res.TypedArray) {
        val windowAnimationStyleResId = styledAttributes.getResourceId(0, DEFAULT_RESOURCE_ID)
        if (windowAnimationStyleResId == DEFAULT_RESOURCE_ID) return

        val windowAnimationStyle = resources.newTheme()
        windowAnimationStyle.applyStyle(windowAnimationStyleResId, false)
        val animAttrs = windowAnimationStyle.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.activityOpenEnterAnimation,
                android.R.attr.activityOpenExitAnimation,
                android.R.attr.activityCloseEnterAnimation,
                android.R.attr.activityCloseExitAnimation
            )
        )
        try {
            window.setWindowAnimations(DEFAULT_RESOURCE_ID)
        } finally {
            animAttrs.recycle()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.getQueryParameter("code")?.let { code ->
                Timber.d("Processing authentication code: ${code.take(AUTH_CODE_LOG_LENGTH)}...")
                lifecycleScope.launch {
                    mainViewModel.handleAuthCallback(code)
                    mainViewModel.checkAuthentication()
                }
            } ?: Timber.e("Authentication code not found in URI: ${intent.data}")
        }
    }
}

@Composable
private fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val mainUiState by mainViewModel.uiState.collectAsState()

    val isAuthenticated = mainUiState.isAuthenticated
    if (isAuthenticated == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val startDestination = if (isAuthenticated) "track" else "auth"
        NavHost(navController = navController, startDestination = startDestination) {
            composable("auth") {
                AuthScreen(uiState = mainUiState, onLoginClick = { mainViewModel.startAuth() })
            }
            composable("track") {
                val trackViewModel: TrackViewModel = hiltViewModel()
                val trackUiState by trackViewModel.uiState.collectAsState()
                TrackScreen(
                    viewModel = trackViewModel,
                    uiState = trackUiState,
                    onRecordEpisode = { id, workId, status ->
                        trackViewModel.recordEpisode(
                            id,
                            workId,
                            status
                        )
                    },
                    onNavigateToHistory = { navController.navigate("history") },
                    onRefresh = { trackViewModel.refresh() }
                )
            }
            composable("history") {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                val historyUiState by historyViewModel.uiState.collectAsState()
                val actions = HistoryScreenActions(
                    onNavigateBack = { navController.navigateUp() },
                    onRetry = { historyViewModel.loadRecords() },
                    onDeleteRecord = { historyViewModel.deleteRecord(it) },
                    onRefresh = { historyViewModel.loadRecords() },
                    onLoadNextPage = { historyViewModel.loadNextPage() },
                    onSearchQueryChange = { historyViewModel.updateSearchQuery(it) },
                    onRecordClick = { historyViewModel.showRecordDetail(it) },
                    onDismissRecordDetail = { historyViewModel.hideRecordDetail() }
                )
                HistoryScreen(uiState = historyUiState, actions = actions)
            }
        }
    }
}
