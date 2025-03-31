package com.zelretch.aniiiiiict.ui.main

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.core.net.toUri

data class MainUiState(
    val programs: List<ProgramWithWork> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDateTime = LocalDateTime.now()
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
        viewModelScope.launch {
            ErrorLogger.logInfo("プログラム一覧の取得を開始", "loadPrograms")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // GraphQLでプログラム一覧を取得
                println("MainViewModel: GraphQLでプログラム一覧の取得を開始")
                repository.getProgramsWithWorks()
                    .catch { e ->
                        ErrorLogger.logError(e, "プログラム一覧の取得に失敗")
                        println("MainViewModel: プログラム一覧の取得に失敗 - ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            error = e.message ?: "プログラム一覧の取得に失敗しました",
                            isLoading = false
                        )
                    }
                    .collect { programs ->
                        ErrorLogger.logInfo("プログラム一覧の取得に成功: ${programs.size}件", "loadPrograms")
                        println("MainViewModel: プログラム一覧の取得に成功 - ${programs.size}件")
                        _uiState.value = _uiState.value.copy(
                            programs = programs,
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                ErrorLogger.logError(e, "プログラム一覧の取得に失敗 - ${_uiState.value.selectedDate}")
                println("MainViewModel: 例外発生 - ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "プログラム一覧の取得に失敗しました",
                    isLoading = false
                )
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
        viewModelScope.launch {
            try {
                ErrorLogger.logInfo("認証URLを取得中", "startAuth")
                val authUrl = repository.getAuthUrl()
                ErrorLogger.logInfo("認証URLを取得: $authUrl", "startAuth")
                val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                ErrorLogger.logError(e, "認証URLの取得に失敗")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証に失敗しました",
                    isLoading = false
                )
            }
        }
    }

    fun handleAuthCallback(code: String) {
        viewModelScope.launch {
            try {
                repository.handleAuthCallback(code)
                loadPrograms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証に失敗しました"
                )
            }
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
}