package com.zelretch.aniiiiict

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.base.ErrorMapper
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
    private lateinit var errorMapper: ErrorMapper
    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authUseCase = mockk(relaxUnitFun = true)
        context = mockk()
        customTabsIntent = mockk(relaxUnitFun = true)
        customTabsIntentFactory = mockk()
        errorMapper = mockk(relaxed = true)
        every { customTabsIntentFactory.create() } returns customTabsIntent
        every { customTabsIntent.launchUrl(any(), any()) } just Runs

        coEvery { authUseCase.isAuthenticated() } returns false

        viewModel = MainViewModel(authUseCase, customTabsIntentFactory, errorMapper, context)
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
        fun notAuthenticated() {
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
        fun checkAuthOnInit() {
            // Given
            coEvery { authUseCase.isAuthenticated() } returns true

            // When
            val newViewModel = MainViewModel(authUseCase, customTabsIntentFactory, errorMapper, context)
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
        fun startAuth() {
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
        fun withValidCode() {
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
        fun withInvalidCode() {
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
        fun whenAuthenticated() {
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
        fun whenNotAuthenticated() {
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
        fun checkingAuth() {
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
    @DisplayName("認証キャンセル")
    inner class CancelAuth {

        @Test
        @DisplayName("認証をキャンセルするとisAuthenticatingがfalseになる")
        fun cancelAuth() {
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
