package com.zelretch.aniiiiict

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MainViewModel")
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authUseCase: AnnictAuthUseCase
    private lateinit var context: Context
    private lateinit var customTabsIntent: CustomTabsIntent
    private lateinit var customTabsIntentFactory: CustomTabsIntentFactory
    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authUseCase = mockk(relaxUnitFun = true)
        context = mockk()
        customTabsIntent = mockk(relaxUnitFun = true)
        customTabsIntentFactory = mockk()
        every { customTabsIntentFactory.create() } returns customTabsIntent
        every { customTabsIntent.launchUrl(any(), any()) } just Runs

        coEvery { authUseCase.isAuthenticated() } returns false

        viewModel = MainViewModel(authUseCase, customTabsIntentFactory, context)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初期状態")
    inner class InitialState {

        @Test
        @DisplayName("認証されていない状態で初期化される")
        fun 認証されていない状態で初期化される() {
            // When
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isAuthenticating)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        @DisplayName("初期化時に認証状態がチェックされる")
        fun 初期化時に認証状態がチェックされる() {
            // Given
            coEvery { authUseCase.isAuthenticated() } returns true

            // When
            val newViewModel = MainViewModel(authUseCase, customTabsIntentFactory, context)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertTrue(newViewModel.uiState.value.isAuthenticated)
            assertFalse(newViewModel.uiState.value.isLoading)
            coVerify { authUseCase.isAuthenticated() }
        }
    }

    @Nested
    @DisplayName("認証開始")
    inner class StartAuth {

        @Test
        @DisplayName("認証開始時にisAuthenticatingがtrueになる")
        fun 認証開始時にisAuthenticatingがtrueになる() {
            // Given
            coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"

            // When
            viewModel.startAuth()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.isAuthenticating)
            coVerify { authUseCase.getAuthUrl() }
        }
    }

    @Nested
    @DisplayName("認証コールバック処理")
    inner class HandleAuthCallback {

        @Test
        @DisplayName("有効なコードで認証が成功する")
        fun 有効なコードで認証が成功する() {
            // Given
            coEvery { authUseCase.handleAuthCallback(any()) } returns true

            // When
            viewModel.handleAuthCallback("valid_code")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isAuthenticating)
            assertNull(viewModel.uiState.value.error)
            coVerify { authUseCase.handleAuthCallback("valid_code") }
        }

        @Test
        @DisplayName("無効なコードでエラーが発生する")
        fun 無効なコードでエラーが発生する() {
            // Given
            coEvery { authUseCase.handleAuthCallback(any()) } returns false

            // When
            viewModel.handleAuthCallback("invalid_code")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isAuthenticating)
            assertNotNull(viewModel.uiState.value.error)
            coVerify { authUseCase.handleAuthCallback("invalid_code") }
        }

        @Test
        @DisplayName("nullのコードでエラーが発生する")
        fun nullのコードでエラーが発生する() {
            // When
            viewModel.handleAuthCallback(null)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isAuthenticating)
            assertNotNull(viewModel.uiState.value.error)
        }
    }

    @Nested
    @DisplayName("認証状態の確認")
    inner class CheckAuthentication {

        @Test
        @DisplayName("認証済みの場合trueになる")
        fun 認証済みの場合trueになる() {
            // Given
            coEvery { authUseCase.isAuthenticated() } returns true

            // When
            viewModel.checkAuthentication()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value.isAuthenticated)
            coVerify { authUseCase.isAuthenticated() }
        }

        @Test
        @DisplayName("未認証の場合falseになる")
        fun 未認証の場合falseになる() {
            // Given
            coEvery { authUseCase.isAuthenticated() } returns false

            // When
            viewModel.checkAuthentication()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isAuthenticated)
            coVerify { authUseCase.isAuthenticated() }
        }

        @Test
        @DisplayName("認証状態確認中はローディング状態になる")
        fun 認証状態確認中はローディング状態になる() {
            // Given
            coEvery { authUseCase.isAuthenticated() } returns true

            // When
            viewModel.checkAuthentication()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertFalse(viewModel.uiState.value.isLoading) // After completion, loading should be false
            assertTrue(viewModel.uiState.value.isAuthenticated)
            coVerify { authUseCase.isAuthenticated() }
        }
    }

    @Nested
    @DisplayName("エラー処理")
    inner class ErrorHandling {

        @Test
        @DisplayName("エラーをクリアできる")
        fun エラーをクリアできる() {
            // Given
            viewModel.updateErrorState("テストエラー")
            assertNotNull(viewModel.uiState.value.error)

            // When
            viewModel.clearError()

            // Then
            assertNull(viewModel.uiState.value.error)
        }
    }

    @Nested
    @DisplayName("ローディング状態")
    inner class LoadingState {

        @Test
        @DisplayName("ローディング状態を更新できる")
        fun ローディング状態を更新できる() {
            // When
            viewModel.updateLoadingState(true)

            // Then
            assertTrue(viewModel.uiState.value.isLoading)
        }
    }

    @Nested
    @DisplayName("認証キャンセル")
    inner class CancelAuth {

        @Test
        @DisplayName("認証をキャンセルするとisAuthenticatingがfalseになる")
        fun 認証をキャンセルするとisAuthenticatingがfalseになる() {
            // Given: 最初に認証を開始してisAuthenticatingをtrueにする
            coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"
            viewModel.startAuth()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAuthenticating)

            // When: 認証をキャンセル
            viewModel.cancelAuth()

            // Then
            assertFalse(viewModel.uiState.value.isAuthenticating)
        }
    }
}
