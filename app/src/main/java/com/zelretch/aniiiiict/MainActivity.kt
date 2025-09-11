package com.zelretch.aniiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Track : Screen("track", "作品一覧", Icons.Default.List)
    object History : Screen("history", "記録履歴", Icons.Default.History)
    object Settings : Screen("settings", "設定", Icons.Default.Settings)
}

@Composable
private fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val mainUiState by mainViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(Screen.Track, Screen.History, Screen.Settings)
    val selectedItem = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = item.route == selectedItem,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "auth") {
            composable("auth") {
                LaunchedEffect(mainUiState.isAuthenticated) {
                    if (mainUiState.isAuthenticated) {
                        navController.navigate("track") { popUpTo("auth") { inclusive = true } }
                    }
                }
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
                    onRefresh = { trackViewModel.refresh() },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
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
            composable("settings") {
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
