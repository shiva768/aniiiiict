package com.zelretch.aniiiiiict

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zelretch.aniiiiiict.ui.history.HistoryScreen
import com.zelretch.aniiiiiict.ui.history.HistoryViewModel
import com.zelretch.aniiiiiict.ui.main.MainScreen
import com.zelretch.aniiiiiict.ui.main.MainViewModel
import com.zelretch.aniiiiiict.ui.theme.AniiiiictTheme
import com.zelretch.aniiiiiict.util.ErrorLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ViewModelを一度だけ初期化して保持する
    private val viewModel: MainViewModel by viewModels()
    private var pendingAuthCode: String? = null
    private var isProcessingAuth = false
    private var isResumeFromBrowser = false
    private var authCodeProcessed = false

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

        // 認証コードを抽出して保存
        extractAuthCode(intent)

        setContent {
            AniiiiictTheme {
                val navController = rememberNavController()
                var authCodeProcessed by remember { mutableStateOf(false) }

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        val viewModel = hiltViewModel<MainViewModel>()
                        val uiState by viewModel.uiState.collectAsState()

                        LaunchedEffect(authCodeProcessed) {
                            if (authCodeProcessed) {
                                ErrorLogger.logInfo("認証コード処理後のプログラム読み込みを実行", "MainActivity.LaunchedEffect")
                                viewModel.loadPrograms()
                                authCodeProcessed = false
                            }
                        }

                        LaunchedEffect(Unit) {
                            handleIntent(intent, onAuthProcessed = { 
                                ErrorLogger.logInfo("認証処理完了フラグをセット", "MainActivity.handleIntent.callback")
                                authCodeProcessed = true 
                            })
                            viewModel.loadPrograms()
                        }

                        MainScreen(
                            uiState = uiState,
                            onImageLoad = { id, url -> viewModel.onImageLoad(id, url) },
                            onRecordEpisode = { id -> viewModel.recordEpisode(id) },
                            onNavigateToHistory = { navController.navigate("history") },
                            onRefresh = { viewModel.refresh() }
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
                            onRefresh = { historyViewModel.loadRecords() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent, null)
    }

    override fun onResume() {
        super.onResume()

        // ブラウザから戻ってきた場合は、状態を更新
        if (isResumeFromBrowser) {
            Log.d("MainActivity", "ブラウザから復帰しました")
            isResumeFromBrowser = false

            // アプリがフォアグラウンドに戻った時点で認証コードがない場合は
            // 認証をキャンセルしたと判断
            if (pendingAuthCode == null && viewModel.uiState.value.isAuthenticating) {
                Log.d("MainActivity", "認証がキャンセルされました")
                viewModel.cancelAuth()
            }
        }
    }

    private fun extractAuthCode(intent: Intent) {
        Log.d("MainActivity", "Intent解析中: ${intent.action}, data: ${intent.data}")
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri?.scheme == "aniiiiiict" && uri.host == "oauth" && uri.pathSegments.firstOrNull() == "callback") {
                val code = uri.getQueryParameter("code")
                Log.d("MainActivity", "認証コードを抽出: ${code?.take(5)}...")
                if (code != null && pendingAuthCode == null && !isProcessingAuth) {
                    pendingAuthCode = code
                    ErrorLogger.logInfo("認証コードを保存: ${code.take(5)}...", "extractAuthCode")
                } else {
                    Log.d(
                        "MainActivity",
                        "既に認証コードが処理中のため、新しいコードをスキップします"
                    )
                }
            }
        }
    }

    private fun handleIntent(intent: Intent, onAuthProcessed: (() -> Unit)? = null) {
        Log.d("MainActivity", "Intent received: ${intent.action}, data: ${intent.data}")
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                Log.d("MainActivity", "Received OAuth callback: $uri")
                // Extract the auth code from the URI
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    if (!isProcessingAuth) {
                        isProcessingAuth = true
                        Log.d("MainActivity", "Processing authentication code: ${code.take(5)}...")
                        ErrorLogger.logInfo(
                            "Processing authentication code: ${code.take(5)}...",
                            "handleIntent"
                        )

                        // 認証処理は新しいコルーチンで実行して独立性を保つ
                        lifecycleScope.launch {
                            viewModel.handleAuthCallback(code)
                            isProcessingAuth = false
                            
                            // コールバックがある場合は実行
                            onAuthProcessed?.invoke()
                        }
                    } else {
                        Log.d(
                            "MainActivity",
                            "認証処理が既に進行中です。このリクエストはスキップします。"
                        )
                        ErrorLogger.logInfo(
                            "認証処理が既に進行中です。このリクエストはスキップします。",
                            "handleIntent"
                        )
                    }
                } else {
                    Log.e("MainActivity", "認証コードがURIに含まれていません: $uri")
                    ErrorLogger.logError("認証コードがURIに含まれていません: $uri", "handleIntent")
                }
            }
        }
    }
} 