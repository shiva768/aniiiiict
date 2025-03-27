package com.zelretch.aniiiiiict.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.dao.CustomStartDateDao
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.util.ErrorLogger
import com.zelretch.aniiiiiict.util.NetworkMonitor
import com.zelretch.aniiiiiict.util.RetryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiClient: AnnictApiClient,
    private val authManager: AnnictAuthManager,
    private val tokenManager: TokenManager,
    private val customStartDateDao: CustomStartDateDao,
    private val repository: AnnictRepository,
    private val networkMonitor: NetworkMonitor,
    private val retryManager: RetryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _watchingWorks = MutableStateFlow<List<WorkWithCustomDate>>(emptyList())
    val watchingWorks: StateFlow<List<WorkWithCustomDate>> = _watchingWorks.asStateFlow()

    private val _wantToWatchWorks = MutableStateFlow<List<WorkWithCustomDate>>(emptyList())
    val wantToWatchWorks: StateFlow<List<WorkWithCustomDate>> = _wantToWatchWorks.asStateFlow()

    private var lastAction: (() -> Unit)? = null

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                if (repository.isAuthenticated()) {
                    _uiState.value = _uiState.value.copy(isAuthenticated = true)
                    loadWorks()
                } else {
                    val authUrl = repository.getAuthUrl()
                    _uiState.value = _uiState.value.copy(authUrl = authUrl)
                }
            } catch (e: Exception) {
                ErrorLogger.logError(e, "認証状態の確認")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証状態の確認に失敗しました"
                )
            }
        }
    }

    fun loadWorks() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val works = repository.getWorks()
                _uiState.value = _uiState.value.copy(
                    works = works,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "作品の読み込みに失敗しました",
                    isLoading = false
                )
            }
        }
    }

    fun startAuth() {
        viewModelScope.launch {
            try {
                val authUrl = repository.getAuthUrl()
                _uiState.value = _uiState.value.copy(authUrl = authUrl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証URLの取得に失敗しました"
                )
            }
        }
    }

    fun handleAuthCallback(code: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                repository.handleAuthCallback(code)
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    isLoading = false
                )
                loadWorks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "認証に失敗しました",
                    isLoading = false
                )
            }
        }
    }

    fun setCustomStartDate(workId: Long, date: LocalDateTime) {
        viewModelScope.launch {
            try {
                repository.setCustomStartDate(workId, date)
                loadWorks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "開始日の設定に失敗しました"
                )
            }
        }
    }

    fun clearCustomStartDate(workId: Long) {
        viewModelScope.launch {
            try {
                repository.clearCustomStartDate(workId)
                loadWorks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "開始日の削除に失敗しました"
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

data class MainUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val works: List<AnnictWork> = emptyList(),
    val authUrl: String? = null
)

data class WorkWithCustomDate(
    val work: AnnictWork,
    val customStartDate: LocalDateTime?
) {
    val effectiveStartDate: LocalDateTime?
        get() = customStartDate ?: work.releasedOn?.takeIf { it.isNotEmpty() }?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
} 