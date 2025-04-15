package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AnnictRepository,
    logger: Logger,
    @ApplicationContext private val context: Context
) : BaseViewModel(logger) {
    private val TAG = "MainViewModel"

    // UI状態のカプセル化
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkAuthState()
        }
    }

    override fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        _uiState.update { it.copy(error = error) }
    }

    // 認証状態の確認（内部メソッド）
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val isAuthenticated = repository.isAuthenticated()

                // 認証されていない場合は認証を開始
                if (!isAuthenticated) {
                    logger.logInfo(
                        TAG,
                        "認証されていないため、認証を開始します",
                        "checkAuthState"
                    )
                    startAuth()
                }
            } catch (e: Exception) {
                logger.logError(TAG, e, "認証状態の確認中にエラーが発生")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "認証状態の確認に失敗しました",
                        isLoading = false
                    )
                }
            }
        }
    }

    // 認証開始（公開メソッド）
    fun startAuth() {
        _uiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                val authUrl = repository.getAuthUrl()
                logger.logInfo(TAG, "認証URLを取得: $authUrl", "startAuth")

                delay(200)

                if (!isActive) return@launch

                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                logger.logError(TAG, e, "認証URLの取得に失敗")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "認証に失敗しました",
                        isLoading = false,
                        isAuthenticating = false
                    )
                }
            }
        }
    }

    // 認証コールバック処理（公開メソッド）
    fun handleAuthCallback(code: String?) {
        viewModelScope.launch {
            try {
                if (code != null) {
                    println("MainViewModel: 認証コードを処理中: ${code.take(5)}...")
                    delay(200)

                    if (!isActive) return@launch

                    val success = repository.handleAuthCallback(code)
                    if (success) {
                        println("MainViewModel: 認証成功")
                        delay(300)
                        _uiState.update { it.copy(isAuthenticating = false) }
                    } else {
                        logger.logWarning(
                            TAG,
                            "認証が失敗しました",
                            "handleAuthCallback"
                        )
                        println("MainViewModel: 認証失敗")
                        delay(200)
                        _uiState.update {
                            it.copy(
                                error = "認証に失敗しました。再度お試しください。",
                                isLoading = false,
                                isAuthenticating = false
                            )
                        }
                    }
                } else {
                    logger.logWarning(
                        TAG,
                        "認証コードがnullです",
                        "handleAuthCallback"
                    )
                    println("MainViewModel: 認証コードなし")
                    delay(200)
                    _uiState.update {
                        it.copy(
                            error = "認証に失敗しました。再度お試しください。",
                            isLoading = false,
                            isAuthenticating = false
                        )
                    }
                }
            } catch (e: Exception) {
                logger.logError(TAG, e, "認証処理に失敗")
                e.printStackTrace()
                delay(200)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "認証に失敗しました",
                        isLoading = false,
                        isAuthenticating = false
                    )
                }
            }
        }
    }

    // エラー処理（内部メソッド）
    private fun handleError(error: Throwable) {
        logger.logError(TAG, error, "MainViewModel")
        _uiState.update { it.copy(error = error.message) }
    }

    // エラーをクリアする
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}