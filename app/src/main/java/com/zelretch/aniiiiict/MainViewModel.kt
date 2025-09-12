package com.zelretch.aniiiiict

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiict.ui.base.BaseUiState
import com.zelretch.aniiiiict.ui.base.BaseViewModel
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.base.ErrorHandler
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
    @param:ApplicationContext private val context: Context
) : BaseViewModel(), MainViewModelContract {

    companion object {
        private const val AUTH_URL_FETCH_DELAY_MS = 200L
        private const val AUTH_CODE_LOG_LENGTH = 5
        private const val AUTH_CALLBACK_DELAY_MS = 200L
        private const val AUTH_SUCCESS_DELAY_MS = 300L
    }

    // UI状態のカプセル化
    internal val internalUiState = MutableStateFlow(MainUiState())
    override val uiState: StateFlow<MainUiState> = internalUiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkAuthState()
        }
    }

    override fun updateLoadingState(isLoading: Boolean) {
        internalUiState.update { it.copy(isLoading = isLoading) }
    }

    override fun updateErrorState(error: String?) {
        internalUiState.update { it.copy(error = error) }
    }

    // 認証状態の確認（内部メソッド）
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val isAuthenticated = annictAuthUseCase.isAuthenticated()

                // UI状態を更新
                internalUiState.update { it.copy(isAuthenticated = isAuthenticated) }

                // 認証されていない場合は認証を開始
                if (!isAuthenticated) {
                    Timber.i("認証されていないため、認証を開始します")
                    // 自動認証は行わず、ユーザーが明示的に認証を開始するのを待つ
                }
            } catch (e: Exception) {
                val msg = ErrorHandler.handleError(e, "MainViewModel", "checkAuthState")
                internalUiState.update {
                    it.copy(
                        error = msg,
                        isLoading = false
                    )
                }
            }
        }
    }

    // 認証開始（公開メソッド）
    override fun startAuth() {
        internalUiState.update { it.copy(isAuthenticating = true) }

        viewModelScope.launch {
            try {
                val authUrl = annictAuthUseCase.getAuthUrl()
                Timber.i("認証URLを取得: $authUrl", "startAuth")

                delay(AUTH_URL_FETCH_DELAY_MS)

                if (!isActive) return@launch

                // Custom Tabsを使用して認証ページを開く
                val customTabsIntent = customTabsIntentFactory.create()
                customTabsIntent.launchUrl(context, authUrl.toUri())
            } catch (e: Exception) {
                val msg = ErrorHandler.handleError(e, "MainViewModel", "startAuth")
                internalUiState.update {
                    it.copy(
                        error = msg,
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
                    Timber.d("MainViewModel: 認証コードを処理中: ${code.take(AUTH_CODE_LOG_LENGTH)}...")
                    delay(AUTH_CALLBACK_DELAY_MS)

                    if (!isActive) return@launch

                    val success = annictAuthUseCase.handleAuthCallback(code)
                    if (success) {
                        Timber.d("MainViewModel: 認証成功")
                        delay(AUTH_SUCCESS_DELAY_MS)
                        internalUiState.update {
                            it.copy(isAuthenticating = false, isAuthenticated = true)
                        }
                    } else {
                        Timber.w("認証が失敗しました")
                        Timber.d("MainViewModel: 認証失敗")
                        delay(AUTH_CALLBACK_DELAY_MS)
                        internalUiState.update {
                            it.copy(
                                error = "認証に失敗しました。再度お試しください。",
                                isLoading = false,
                                isAuthenticating = false,
                                isAuthenticated = false
                            )
                        }
                    }
                } else {
                    Timber.w("認証コードがnullです")
                    delay(AUTH_CALLBACK_DELAY_MS)
                    internalUiState.update {
                        it.copy(
                            error = "認証に失敗しました。再度お試しください。",
                            isLoading = false,
                            isAuthenticating = false
                        )
                    }
                }
            } catch (e: Exception) {
                val msg = ErrorHandler.handleError(e, "MainViewModel", "handleAuthCallback")
                delay(AUTH_CALLBACK_DELAY_MS)
                internalUiState.update {
                    it.copy(
                        error = msg,
                        isLoading = false,
                        isAuthenticating = false
                    )
                }
            }
        }
    }

    // エラーをクリアする
    override fun clearError() {
        internalUiState.update { it.copy(error = null) }
    }

    // 認証状態を手動で確認する（公開メソッド）
    override fun checkAuthentication() {
        checkAuthState()
    }
}
