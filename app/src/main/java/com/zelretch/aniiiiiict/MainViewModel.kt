package com.zelretch.aniiiiiict

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MainUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isAuthenticating: Boolean = false,
    val isAuthenticated: Boolean = false
) : BaseUiState(isLoading, error)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val annictAuthUseCase: AnnictAuthUseCase,
    private val customTabsIntentFactory: CustomTabsIntentFactory,
    @ApplicationContext private val context: Context
) : BaseViewModel(), MainViewModelContract {
    // UI状態のカプセル化
    internal val _uiState = MutableStateFlow(MainUiState())
    override val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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
                val isAuthenticated = annictAuthUseCase.isAuthenticated()

                // UI状態を更新
                _uiState.update { it.copy(isAuthenticated = isAuthenticated) }

                // 認証されていない場合は認証を開始
                if (!isAuthenticated) {
                    Timber.i(
                        "MainViewModel",
                        "認証されていないため、認証を開始します",
                        "checkAuthState"
                    )
                    // 自動認証は行わず、ユーザーが明示的に認証を開始するのを待つ
                }
            } catch (e: Exception) {
                Timber.e("MainViewModel", e, "認証状態の確認中にエラーが発生")
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
    override fun startAuth() {
        _uiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                val authUrl = annictAuthUseCase.getAuthUrl()
                Timber.i("MainViewModel", "認証URLを取得: $authUrl", "startAuth")

                delay(200)

                if (!isActive) return@launch

                // Custom Tabsを使用して認証ページを開く
                val customTabsIntent = customTabsIntentFactory.create()
                customTabsIntent.launchUrl(context, authUrl.toUri())
            } catch (e: Exception) {
                Timber.e("MainViewModel", e, "認証URLの取得に失敗")
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
    override fun handleAuthCallback(code: String?) {
        viewModelScope.launch {
            try {
                if (code != null) {
                    println("MainViewModel: 認証コードを処理中: ${code.take(5)}...")
                    delay(200)

                    if (!isActive) return@launch

                    val success = annictAuthUseCase.handleAuthCallback(code)
                    if (success) {
                        println("MainViewModel: 認証成功")
                        delay(300)
                        _uiState.update {
                            it.copy(isAuthenticating = false, isAuthenticated = true)
                        }
                    } else {
                        Timber.w(
                            "MainViewModel",
                            "認証が失敗しました",
                            "handleAuthCallback"
                        )
                        println("MainViewModel: 認証失敗")
                        delay(200)
                        _uiState.update {
                            it.copy(
                                error = "認証に失敗しました。再度お試しください。",
                                isLoading = false,
                                isAuthenticating = false,
                                isAuthenticated = false
                            )
                        }
                    }
                } else {
                    Timber.w(
                        "MainViewModel",
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
                Timber.e("MainViewModel", e, "認証処理に失敗")
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
        Timber.e("MainViewModel", error, "MainViewModel")
        _uiState.update { it.copy(error = error.message) }
    }

    // エラーをクリアする
    override fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // 認証状態を手動で確認する（公開メソッド）
    override fun checkAuthentication() {
        checkAuthState()
    }
}
