package com.zelretch.aniiiiiict

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // 認証コードがある場合のみ実行（パフォーマンス向上）
                    val needsProcessAuthCode = remember(pendingAuthCode) { pendingAuthCode != null }
                    
                    if (needsProcessAuthCode) {
                        LaunchedEffect(Unit) {
                            pendingAuthCode?.let { code ->
                                if (!isProcessingAuth) {
                                    isProcessingAuth = true
                                    Log.d("MainActivity", "処理待ちの認証コードを処理します: ${code.take(5)}...")
                                    ErrorLogger.logInfo("処理待ちの認証コードを処理します: ${code.take(5)}...", "LaunchedEffect")
                                    
                                    // 認証処理は新しいコルーチンで実行して独立性を保つ
                                    lifecycleScope.launch {
                                        viewModel.handleAuthCallback(code)
                                        pendingAuthCode = null
                                        isProcessingAuth = false
                                    }
                                }
                            }
                        }
                    }

                    MainScreen(
                        uiState = uiState,
                        onProgramClick = viewModel::onProgramClick,
                        onDateChange = viewModel::onDateChange,
                        onImageLoad = viewModel::onImageLoad
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // 現在のインテントを設定して、新しいインテントを処理
        setIntent(intent)
        isResumeFromBrowser = true
        
        if (!isProcessingAuth) {
            handleIntent(intent)
        } else {
            Log.d("MainActivity", "認証処理中のため、新しいインテントをスキップします")
        }
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
                    Log.d("MainActivity", "既に認証コードが処理中のため、新しいコードをスキップします")
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d("MainActivity", "Intent received: ${intent.action}, data: ${intent.data}")
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri?.scheme == "aniiiiiict" && uri.host == "oauth" && uri.pathSegments.firstOrNull() == "callback") {
                val code = uri.getQueryParameter("code")
                Log.d("MainActivity", "OAuth callback received with code: ${code?.take(5)}...")
                if (code != null && !isProcessingAuth) {
                    isProcessingAuth = true
                    ErrorLogger.logInfo("認証コードを処理: ${code.take(5)}...", "handleIntent")
                    
                    // 認証処理は新しいコルーチンで実行して独立性を保つ
                    lifecycleScope.launch {
                        viewModel.handleAuthCallback(code)
                        isProcessingAuth = false
                    }
                } else {
                    Log.e("MainActivity", "Failed to handle OAuth callback: code=$code, isProcessingAuth=$isProcessingAuth")
                    if (code == null) {
                        ErrorLogger.logError(Exception("認証コールバックの処理に失敗: コードがnull"), "handleIntent")
                    }
                }
            }
        }
    }
} 