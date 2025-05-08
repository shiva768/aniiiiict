package com.zelretch.aniiiiiict

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiiict.util.Logger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
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
        val repository = mockk<AnnictRepository>(relaxUnitFun = true)
        val logger = mockk<Logger> {
            every { info(any<String>(), any<String>(), any<String>()) } returns Unit
            every { warning(any<String>(), any<String>(), any<String>()) } returns Unit
            every { error(any<String>(), any<String>(), any<String>()) } returns Unit
            every { error(any<String>(), any<Throwable>(), any<String>()) } returns Unit
        }
        val context = mockk<Context>()
        val customTabsIntent = mockk<CustomTabsIntent>(relaxUnitFun = true)
        val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
        every { customTabsIntentFactory.create() } returns customTabsIntent
        every { customTabsIntent.launchUrl(any(), any()) } just Runs
        val viewModel = MainViewModel(repository, customTabsIntentFactory, logger, context)

        beforeTest {
            coEvery { repository.isAuthenticated() } returns false
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
                coEvery { repository.getAuthUrl() } returns "https://example.com/auth"
                viewModel.startAuth()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.error shouldBe null
                viewModel.uiState.value.isAuthenticating shouldBe true
                coVerify { repository.getAuthUrl() }
                verify { logger.info(any<String>(), any<String>(), any<String>()) }
            }
        }

        `when`("認証コードを受け取る") {
            then("有効なコードで認証が成功する") {
                coEvery { repository.handleAuthCallback(any()) } returns true
                viewModel.handleAuthCallback("valid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe true
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldBe null
                coVerify { repository.handleAuthCallback("valid_code") }
            }

            then("無効なコードでエラーが発生する") {
                coEvery { repository.handleAuthCallback(any()) } returns false
                viewModel.handleAuthCallback("invalid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldNotBe null
                coVerify { repository.handleAuthCallback("invalid_code") }
                verify { logger.warning(any<String>(), any<String>(), any<String>()) }
            }

            then("nullのコードでエラーが発生する") {
                viewModel.handleAuthCallback(null)
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldNotBe null
                verify { logger.warning(any<String>(), any<String>(), any<String>()) }
            }
        }

        `when`("認証状態を確認") {
            then("認証済みの場合") {
                coEvery { repository.isAuthenticated() } returns true
                viewModel.checkAuthentication()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe true
                coVerify { repository.isAuthenticated() }
            }

            then("未認証の場合") {
                coEvery { repository.isAuthenticated() } returns false
                viewModel.checkAuthentication()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                coVerify { repository.isAuthenticated() }
                verify { logger.info(any<String>(), any<String>(), any<String>()) }
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
    }
})
