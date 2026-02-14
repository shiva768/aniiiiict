package com.zelretch.aniiiiict

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.base.ErrorMapper
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

/**
 * メイン画面のUI状態
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val isAuthenticated: Boolean = false
)

/**
 * メイン画面のViewModel
 *
 * Now in Android パターンへの移行:
 * - BaseViewModelを削除し、明示的なエラーハンドリング
 * - ErrorMapperによるユーザー向けメッセージ変換
 *
 * Note: 認証フローの複雑さから、現時点では従来のUiStateパターンを維持。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val annictAuthUseCase: AnnictAuthUseCase,
    private val customTabsIntentFactory: CustomTabsIntentFactory,
    private val errorMapper: ErrorMapper,
    @param:ApplicationContext private val context: Context
) : ViewModel(), MainViewModelContract {

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

    // 認証状態の確認（内部メソッド）
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                // 認証状態をチェック中はローディング状態にする
                internalUiState.update { it.copy(isLoading = true) }

                val isAuthenticated = annictAuthUseCase.isAuthenticated()

                // UI状態を更新
                internalUiState.update {
                    it.copy(
                        isAuthenticated = isAuthenticated,
                        isLoading = false
                    )
                }

                // 認証されていない場合は認証を開始
                if (!isAuthenticated) {
                    Timber.i("認証されていないため、認証を開始します")
                    // 自動認証は行わず、ユーザーが明示的に認証を開始するのを待つ
                }
            } catch (e: Exception) {
                val msg = errorMapper.toUserMessage(e, "MainViewModel.checkAuthState")
                internalUiState.update {
                    it.copy(
                        error = msg,
                        isLoading = false
                    )
                }
                Timber.e(e, "認証状態確認に失敗: $msg")
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
                val msg = errorMapper.toUserMessage(e, "MainViewModel.startAuth")
                internalUiState.update {
                    it.copy(
                        error = msg,
                        isLoading = false,
                        isAuthenticating = false
                    )
                }
                Timber.e(e, "認証開始に失敗: $msg")
            }
        }
    }

    // 認証コールバック処理（公開メソッド）
    override fun handleAuthCallback(code: String?) {
        viewModelScope.launch {
            Timber.d("MainViewModel: 認証コードを処理中: ${code?.take(AUTH_CODE_LOG_LENGTH)}...")
            delay(AUTH_CALLBACK_DELAY_MS)

            if (!isActive) return@launch

            annictAuthUseCase.handleAuthCallback(code)
                .onSuccess {
                    Timber.d("MainViewModel: 認証成功")
                    delay(AUTH_SUCCESS_DELAY_MS)
                    internalUiState.update {
                        it.copy(isAuthenticating = false, isAuthenticated = true)
                    }
                }
                .onFailure { e ->
                    val msg = errorMapper.toUserMessage(e, "MainViewModel.handleAuthCallback")
                    delay(AUTH_CALLBACK_DELAY_MS)
                    internalUiState.update {
                        it.copy(
                            error = msg,
                            isLoading = false,
                            isAuthenticating = false,
                            isAuthenticated = false
                        )
                    }
                    Timber.e(e, "認証コールバック処理に失敗: $msg")
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

    // 認証をキャンセルする（公開メソッド）
    override fun cancelAuth() {
        internalUiState.update {
            it.copy(isAuthenticating = false)
        }
        Timber.d("MainViewModel: 認証がキャンセルされました")
    }
}
