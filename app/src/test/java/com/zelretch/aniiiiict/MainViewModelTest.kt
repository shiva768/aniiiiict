package com.zelretch.aniiiiict

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BehaviorSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    given("MainViewModel") {
        val authUseCase = mockk<AnnictAuthUseCase>(relaxUnitFun = true)
        val context = mockk<Context>()
        val customTabsIntent = mockk<CustomTabsIntent>(relaxUnitFun = true)
        val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
        every { customTabsIntentFactory.create() } returns customTabsIntent
        every { customTabsIntent.launchUrl(any(), any()) } just Runs
        val viewModel = MainViewModel(authUseCase, customTabsIntentFactory, context)

        beforeTest {
            coEvery { authUseCase.isAuthenticated() } returns false
        }

        `when`("初期状態") {
            then("認証されていない") {
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldBe null
            }
        }

        `when`("認証を開始") {
            then("isAuthenticatingがtrueになる") {
                coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"
                viewModel.startAuth()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.error shouldBe null
                viewModel.uiState.value.isAuthenticating shouldBe true
                coVerify { authUseCase.getAuthUrl() }
            }
        }

        `when`("認証コードを受け取る") {
            then("有効なコードで認証が成功する") {
                coEvery { authUseCase.handleAuthCallback(any()) } returns true
                viewModel.handleAuthCallback("valid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe true
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldBe null
                coVerify { authUseCase.handleAuthCallback("valid_code") }
            }

            then("無効なコードでエラーが発生する") {
                coEvery { authUseCase.handleAuthCallback(any()) } returns false
                viewModel.handleAuthCallback("invalid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldNotBe null
                coVerify { authUseCase.handleAuthCallback("invalid_code") }
            }

            then("nullのコードでエラーが発生する") {
                viewModel.handleAuthCallback(null)
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldNotBe null
            }
        }

        `when`("認証状態を確認") {
            then("認証済みの場合") {
                coEvery { authUseCase.isAuthenticated() } returns true
                viewModel.checkAuthentication()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe true
                coVerify { authUseCase.isAuthenticated() }
            }

            then("未認証の場合") {
                coEvery { authUseCase.isAuthenticated() } returns false
                viewModel.checkAuthentication()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                coVerify { authUseCase.isAuthenticated() }
            }
        }

        `when`("エラーをクリア") {
            then("エラー状態がリセットされる") {
                viewModel.updateErrorState("テストエラー")
                viewModel.uiState.value.error shouldNotBe null
                viewModel.clearError()
                viewModel.uiState.value.error shouldBe null
            }
        }

        `when`("ローディング状態") {
            then("updateLoadingStateを呼ぶ") {
                viewModel.updateLoadingState(true)
                viewModel.uiState.value.isLoading shouldBe true
            }
        }
    }
})
