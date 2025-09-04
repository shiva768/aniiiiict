package com.zelretch.aniiiiict.ui.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zelretch.aniiiiict.MainUiState
import com.zelretch.aniiiiict.MainViewModel
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun buildMainViewModelWithRepoMock(): Pair<MainViewModel, AnnictRepository> {
        val repo = mockk<AnnictRepository>()
        // default stubs
        coEvery { repo.isAuthenticated() } returns false
        coEvery { repo.getAuthUrl() } returns "https://example.com/auth"
        coEvery { repo.handleAuthCallback(any()) } returns true
        val useCase = AnnictAuthUseCase(repo)
        val factory = mockk<CustomTabsIntentFactory> {
            // Return a relaxed CustomTabsIntent to avoid actual browser launch
            every { create() } returns mockk(relaxed = true)
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        val vm = MainViewModel(useCase, factory, context)
        return vm to repo
    }

    @Test
    fun authScreen_未認証_ログインボタンクリックでgetAuthUrlが呼ばれる() {
        val (vm, repo) = buildMainViewModelWithRepoMock()
        val uiState = MainUiState(isAuthenticating = false, isAuthenticated = false)

        composeRule.setContent {
            AuthScreen(uiState = uiState, onLoginClick = { vm.startAuth() })
        }

        composeRule.onNodeWithText("Annictでログイン").assertIsDisplayed().performClick()
        // Wait for coroutine to run and mock to be called
        composeRule.waitUntil(timeoutMillis = 400) {
            try {
                coVerify(atLeast = 1) { repo.getAuthUrl() }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_コールバック成功_handleAuthCallbackが呼ばれる() {
        val (vm, repo) = buildMainViewModelWithRepoMock()
        vm.handleAuthCallback("CODE123")
        // Wait for coroutine to run and mock to be called
        composeRule.waitUntil(timeoutMillis = 500) {
            try {
                coVerify(exactly = 1) { repo.handleAuthCallback("CODE123") }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_起動時認証確認_isAuthenticatedが呼ばれる() {
        val (vm, repo) = buildMainViewModelWithRepoMock()
        vm.checkAuthentication()
        coVerify(atLeast = 1) { repo.isAuthenticated() }
        // It can be called by init{} and our explicit check
    }
}
