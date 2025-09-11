package com.zelretch.aniiiiict.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiict.ui.history.HistoryScreenActions
import com.zelretch.aniiiiict.ui.history.HistoryViewModel
import com.zelretch.aniiiiict.ui.settings.SettingsScreen
import com.zelretch.aniiiiict.ui.works.WorksScreen
import com.zelretch.aniiiiict.ui.works.WorksViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Works : Screen("works", "作品", Icons.AutoMirrored.Filled.List)
    object History : Screen("history", "履歴", Icons.Filled.History)
    object Settings : Screen("settings", "設定", Icons.Filled.Settings)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(
                    Screen.Works,
                    Screen.History,
                    Screen.Settings
                )
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
        NavHost(
            navController = navController,
            startDestination = Screen.Works.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Works.route) {
                val worksViewModel: WorksViewModel = hiltViewModel()
                val worksUiState by worksViewModel.uiState.collectAsState()
                WorksScreen(
                    viewModel = worksViewModel,
                    uiState = worksUiState,
                    onRecordEpisode = { id, workId, status -> worksViewModel.recordEpisode(id, workId, status) },
                    onRefresh = { worksViewModel.refresh() }
                )
            }
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                val historyUiState by historyViewModel.uiState.collectAsState()
                val actions = HistoryScreenActions(
                    onRetry = { historyViewModel.loadRecords() },
                    onDeleteRecord = { historyViewModel.deleteRecord(it) },
                    onRefresh = { historyViewModel.loadRecords() },
                    onLoadNextPage = { historyViewModel.loadNextPage() },
                    onSearchQueryChange = { historyViewModel.updateSearchQuery(it) }
                )
                HistoryScreen(uiState = historyUiState, actions = actions)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
