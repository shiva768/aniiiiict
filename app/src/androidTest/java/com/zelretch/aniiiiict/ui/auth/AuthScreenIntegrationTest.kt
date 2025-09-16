package com.zelretch.aniiiiict.ui.auth

import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.zelretch.aniiiiict.MainUiState
import com.zelretch.aniiiiict.MainViewModel
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiict.testing.ヒルトコンポーズテストルール
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * AuthScreenの統合テスト。
 * UI操作からMainViewModel、AnnictAuthUseCaseを経由し、Repository（モック）が
 * 正しく呼び出されるかという、コンポーネント間の連携を検証する。
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class 認証画面統合テスト {

    @get:Rule
    val testRule = ヒルトコンポーズテストルール(this)

    @Inject
    lateinit var annictAuthUseCase: AnnictAuthUseCase

    @BindValue
    @JvmField
    val annictRepository: AnnictRepository = mockk<AnnictRepository>().apply {
        coEvery { isAuthenticated() } returns false
        coEvery { getAuthUrl() } returns "https://example.com/auth"
        coEvery { handleAuthCallback(any()) } returns true
    }

    @BindValue
    @JvmField
    val aniListRepository: AniListRepository = mockk<AniListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val myAnimeListRepository: MyAnimeListRepository = mockk<MyAnimeListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val programFilter: ProgramFilter = mockk<ProgramFilter>(relaxed = true)

    @BindValue
    @JvmField
    val customTabsIntentFactory: CustomTabsIntentFactory = mockk<CustomTabsIntentFactory>().apply {
        every { create() } returns mockk(relaxed = true)
    }

    @Test
    fun authScreen_ログインボタンクリック_getAuthUrlが呼ばれる() {
        // Arrange
        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)
        val uiState = MainUiState(isAuthenticating = false, isAuthenticated = false)

        // Act
        testRule.composeTestRule.setContent {
            AuthScreen(uiState = uiState, onLoginClick = { viewModel.startAuth() })
        }

        testRule.composeTestRule.onNodeWithText("Annictでログイン").performClick()

        // Assert - Wait for coroutine to run and mock to be called
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(atLeast = 1) { annictRepository.getAuthUrl() }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_コールバック処理_handleAuthCallbackが呼ばれる() {
        // Arrange
        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)

        // Act - handleAuthCallback を直接呼び出し
        viewModel.handleAuthCallback("CODE123")

        // Assert - Wait for coroutine to run and mock to be called
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(exactly = 1) { annictRepository.handleAuthCallback("CODE123") }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_認証状態確認_isAuthenticatedが呼ばれる() {
        // Arrange
        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)

        // Act - 認証状態確認を実行
        viewModel.checkAuthentication()

        // Assert - Wait for coroutine to run and mock to be called
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(atLeast = 1) { annictRepository.isAuthenticated() }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_認証失敗時_適切にエラーハンドリング() {
        // Arrange
        coEvery { annictRepository.getAuthUrl() } throws RuntimeException("認証エラー")

        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)
        val uiState = MainUiState(isAuthenticating = false, isAuthenticated = false)

        // Act
        testRule.composeTestRule.setContent {
            AuthScreen(uiState = uiState, onLoginClick = { viewModel.startAuth() })
        }

        testRule.composeTestRule.onNodeWithText("Annictでログイン").performClick()

        // Assert - エラーが発生してもクラッシュしない
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(atLeast = 1) { annictRepository.getAuthUrl() }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_無効なコールバック_handleAuthCallbackが呼ばれる() {
        // Arrange
        coEvery { annictRepository.handleAuthCallback("INVALID") } returns false

        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)

        // Act - 無効なコールバックコードで処理
        viewModel.handleAuthCallback("INVALID")

        // Assert
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(exactly = 1) { annictRepository.handleAuthCallback("INVALID") }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_初期化時認証確認_自動的にisAuthenticatedが呼ばれる() {
        // Arrange & Act - ViewModelの初期化（init{}ブロックで認証確認が実行される）
        val context: Context = ApplicationProvider.getApplicationContext()
        MainViewModel(annictAuthUseCase, customTabsIntentFactory, context)

        // Assert - Wait for coroutine to run and mock to be called
        testRule.composeTestRule.waitUntil(timeoutMillis = 1000) {
            try {
                coVerify(atLeast = 1) { annictRepository.isAuthenticated() }
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
