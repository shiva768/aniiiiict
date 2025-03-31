package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.util.ErrorLogger
import com.zelretch.aniiiiiict.util.NetworkMonitor
import com.zelretch.aniiiiiict.util.RetryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class MainUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDateTime = LocalDateTime.now(),
    val isAuthenticating: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: AnnictApiClient,
    private val authManager: AnnictAuthManager,
    private val tokenManager: TokenManager,
    private val repository: AnnictRepository,
    private val networkMonitor: NetworkMonitor,
    private val retryManager: RetryManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()


    private var lastAction: (() -> Unit)? = null
    private var loadProgramsJob: kotlinx.coroutines.Job? = null
    private var isAuthInProgress = false

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            ErrorLogger.logInfo("認証状態を確認中", "checkAuthState")
            try {
                if (!repository.isAuthenticated()) {
                    ErrorLogger.logInfo("未認証のため認証を開始", "checkAuthState")
                    startAuth()
                } else {
                    ErrorLogger.logInfo("認証済みのためプログラム一覧を取得", "checkAuthState")
                    loadPrograms()
                }
            } catch (e: Exception) {
                ErrorLogger.logError(e, "認証状態の確認中にエラーが発生")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証状態の確認に失敗しました",
                    isLoading = false
                )
            }
        }
    }

    private fun loadPrograms() {
        // 既存のジョブが実行中の場合はキャンセル
        loadProgramsJob?.cancel()
        
        loadProgramsJob = viewModelScope.launch {
            // すでにデータが表示されている場合は、新しいデータの取得中にローディング表示しない
            // これにより画面のチラつきを防止
            val showLoading = _uiState.value.programs.isEmpty()
            
            ErrorLogger.logInfo("プログラム一覧の取得を開始", "loadPrograms")
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // GraphQLでプログラム一覧を取得
                println("MainViewModel: GraphQLでプログラム一覧の取得を開始")
                
                // 少し遅延を入れてUIの更新頻度を下げる
                delay(300)
                
                // キャンセルされているかチェック
                if (!currentCoroutineContext().isActive) {
                    println("MainViewModel: ジョブがキャンセルされたため、GraphQLクエリをスキップ")
                    return@launch
                }
                
                repository.getProgramsWithWorks()
                    .catch { e ->
                        if (e is kotlinx.coroutines.CancellationException) {
                            println("MainViewModel: GraphQLクエリがキャンセルされました")
                            return@catch
                        }
                        
                        ErrorLogger.logError(e, "プログラム一覧の取得に失敗")
                        println("MainViewModel: プログラム一覧の取得に失敗 - ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            error = e.message ?: "プログラム一覧の取得に失敗しました",
                            isLoading = false
                        )
                    }
                    .collect { programs ->
                        // キャンセルされているかチェック
                        if (!currentCoroutineContext().isActive) {
                            println("MainViewModel: ジョブがキャンセルされたため、結果の処理をスキップ")
                            return@collect
                        }
                        
                        ErrorLogger.logInfo("プログラム一覧の取得に成功: ${programs.size}件", "loadPrograms")
                        println("MainViewModel: プログラム一覧の取得に成功 - ${programs.size}件")
                        _uiState.value = _uiState.value.copy(
                            programs = programs,
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    println("MainViewModel: GraphQLクエリの実行がキャンセルされました")
                    return@launch
                }
                
                ErrorLogger.logError(e, "プログラム一覧の取得に失敗 - ${_uiState.value.selectedDate}")
                println("MainViewModel: 例外発生 - ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "プログラム一覧の取得に失敗しました",
                    isLoading = false
                )
            } finally {
                // このジョブが終了した場合は参照をクリア
                if (loadProgramsJob == this) {
                    loadProgramsJob = null
                }
            }
        }
    }

    fun onDateChange(date: LocalDateTime) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadPrograms()
    }

    fun onProgramClick(program: ProgramWithWork) {
        viewModelScope.launch {
            try {
                repository.createRecord(program.program.episode.annictId.toLong())
                loadPrograms() // リストを更新
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark episode as watched"
                )
            }
        }
    }

    fun onImageLoad(workId: Int, imageUrl: String) {
        viewModelScope.launch {
            try {
                repository.saveWorkImage(workId.toLong(), imageUrl)
            } catch (e: Exception) {
                ErrorLogger.logError(e, "画像の保存に失敗 - workId: $workId")
            }
        }
    }

    private fun startAuth() {
        if (isAuthInProgress) {
            println("MainViewModel: 認証処理がすでに進行中のため、新しい認証をスキップします")
            return
        }
        
        isAuthInProgress = true
        _uiState.value = _uiState.value.copy(isAuthenticating = true)
        
        viewModelScope.launch {
            try {
                ErrorLogger.logInfo("認証URLを取得中", "startAuth")
                val authUrl = repository.getAuthUrl()
                ErrorLogger.logInfo("認証URLを取得: $authUrl", "startAuth")
                
                // 少し遅延を入れて遷移を安定させる
                delay(200)
                
                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(intent)
            } catch (e: Exception) {
                ErrorLogger.logError(e, "認証URLの取得に失敗")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証に失敗しました",
                    isLoading = false,
                    isAuthenticating = false
                )
                isAuthInProgress = false
            }
        }
    }

    fun handleAuthCallback(code: String) {
        if (isAuthInProgress) {
            viewModelScope.launch {
                try {
                    ErrorLogger.logInfo("認証コードを処理中: ${code.take(5)}...", "handleAuthCallback")
                    println("MainViewModel: 認証コードを処理中: ${code.take(5)}...")
                    
                    // 少し遅延を入れて画面遷移を安定させる
                    delay(200)
                    
                    val success = repository.handleAuthCallback(code)
                    if (success) {
                        ErrorLogger.logInfo("認証成功、プログラム一覧を読み込みます", "handleAuthCallback")
                        println("MainViewModel: 認証成功")
                        
                        // 処理完了後に状態をリセット
                        isAuthInProgress = false
                        
                        // 認証に成功したら、少し待ってからUIを更新
                        delay(300)
                        _uiState.value = _uiState.value.copy(isAuthenticating = false)
                        
                        // プログラム一覧を読み込む
                        loadPrograms()
                    } else {
                        ErrorLogger.logWarning("認証が失敗しました", "handleAuthCallback")
                        println("MainViewModel: 認証失敗")
                        
                        // 少し待ってからUIを更新
                        delay(200)
                        _uiState.value = _uiState.value.copy(
                            error = "認証に失敗しました。再度お試しください。",
                            isLoading = false,
                            isAuthenticating = false
                        )
                        isAuthInProgress = false
                    }
                } catch (e: Exception) {
                    ErrorLogger.logError(e, "認証コールバックの処理中にエラーが発生")
                    println("MainViewModel: 認証処理中に例外が発生 - ${e.message}")
                    e.printStackTrace()
                    
                    // 少し待ってからUIを更新
                    delay(200)
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "認証に失敗しました",
                        isLoading = false,
                        isAuthenticating = false
                    )
                    isAuthInProgress = false
                }
            }
        } else {
            println("MainViewModel: 認証処理が行われていないため、コールバック処理をスキップします")
        }
    }

    fun showError(message: String) {
        ErrorLogger.logWarning(message, "showError")
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun retryLastAction() {
        ErrorLogger.logInfo("最後のアクションを再試行", "retryLastAction")
        lastAction?.invoke()
    }

    /**
     * 認証をキャンセルする
     * ブラウザから戻ってきた時に認証コードがない場合などに呼び出される
     */
    fun cancelAuth() {
        viewModelScope.launch {
            println("MainViewModel: 認証をキャンセルします")
            // 状態をリセット
            isAuthInProgress = false
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                isLoading = false
            )
        }
    }
}