package com.zelretch.aniiiiict.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiict.ui.history.HistoryScreenActions
import com.zelretch.aniiiiict.ui.history.HistoryViewModel
import com.zelretch.aniiiiict.ui.track.TrackScreen
import com.zelretch.aniiiiict.ui.track.TrackViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Track : Screen("track", "作品一覧", Icons.Default.List)
    object History : Screen("history", "記録履歴", Icons.Default.History)
    object Settings : Screen("settings", "設定", Icons.Default.Settings)
}

private val items = listOf(
    Screen.Track,
    Screen.History,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Track.route, Modifier.padding(innerPadding)) {
            composable(Screen.Track.route) {
                val trackViewModel: TrackViewModel = hiltViewModel()
                val trackUiState by trackViewModel.uiState.collectAsState()
                TrackScreen(
                    viewModel = trackViewModel,
                    uiState = trackUiState,
                    onRecordEpisode = { id, workId, status -> trackViewModel.recordEpisode(id, workId, status) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onRefresh = { trackViewModel.refresh() }
                )
            }
            composable(Screen.History.route) {
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
            composable(Screen.Settings.route) {
                // For now, settings screen shows history screen
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
