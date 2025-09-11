package com.zelretch.aniiiiict

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiict.ui.auth.AuthScreen
import com.zelretch.aniiiiict.ui.main.MainScreen
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
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

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            LaunchedEffect(mainUiState.isAuthenticated) {
                if (mainUiState.isAuthenticated) {
                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                }
            }
            AuthScreen(uiState = mainUiState, onLoginClick = { mainViewModel.startAuth() })
        }
        composable("main") {
            MainScreen()
        }
    }
}
