package com.zelretch.aniiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.delay
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
    object Track : Screen("track", "視聴記録", Icons.AutoMirrored.Filled.List)
    object History : Screen("history", "記録履歴", Icons.Default.History)
    object Settings : Screen("settings", "設定", Icons.Default.Settings)
}

@Composable
private fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val mainUiState by mainViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(Screen.History, Screen.Settings)
    val selectedItem = navController.currentBackStackEntryAsState().value?.destination?.route

    // Track drawer state for potential future implementation
    val shouldRestoreDrawerOpen = remember { mutableStateOf(false) }

    // Determine initial destination based on authentication state
    val startDestination = when {
        mainUiState.isLoading -> "loading"
        mainUiState.isAuthenticated -> "track"
        else -> "auth"
    }

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
                            // Remember if drawer was open when navigating from track screen
                            if (selectedItem == "track" && drawerState.isOpen) {
                                shouldRestoreDrawerOpen.value = true
                            }
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
        // Navigate to appropriate screen when authentication state changes
        LaunchedEffect(mainUiState.isAuthenticated, mainUiState.isLoading) {
            val currentRoute = navController.currentDestination?.route
            when {
                !mainUiState.isLoading && mainUiState.isAuthenticated && currentRoute != "track" -> {
                    navController.navigate("track") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                !mainUiState.isLoading && !mainUiState.isAuthenticated && currentRoute != "auth" -> {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // Handle drawer state restoration when returning to track screen
        LaunchedEffect(selectedItem) {
            // If we're returning to track screen and should restore drawer
            if (selectedItem == "track" && shouldRestoreDrawerOpen.value) {
                // Use a coroutine to handle the drawer opening with proper timing
                kotlinx.coroutines.delay(100) // Small delay to ensure screen is rendered
                drawerState.open()
                shouldRestoreDrawerOpen.value = false
            }
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable("loading") {
                LoadingScreen()
            }
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
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "設定画面",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "実装予定",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
