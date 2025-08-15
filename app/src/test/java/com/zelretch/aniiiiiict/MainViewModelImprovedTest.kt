package com.zelretch.aniiiiiict

import android.content.Context
import app.cash.turbine.test
import com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiiict.ui.base.TestableViewModel
import com.zelretch.aniiiiiict.ui.base.ViewModelTestUtils.resetToInitialState
import com.zelretch.aniiiiiict.ui.base.ViewModelTestUtils.setErrorState
import com.zelretch.aniiiiiict.ui.base.ViewModelTestUtils.setLoadingState
import com.zelretch.aniiiiiict.util.Logger
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * MainViewModelの改善されたテストクラス
 * インターフェースベースのテストとHiltテスト機能を活用したテスト容易性の向上デモ
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainViewModelImprovedTest : BehaviorSpec({
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    given("MainViewModel with improved testability") {
        val hiltRule = HiltAndroidRule(this@MainViewModelImprovedTest)
        
        // @BindValueを使用してテスト用の依存関係を注入
        @BindValue @JvmField 
        val mockAuthUseCase: AnnictAuthUseCase = mockk(relaxUnitFun = true)
        
        @BindValue @JvmField
        val mockLogger: Logger = mockk(relaxed = true)
        
        @BindValue @JvmField
        val mockContext: Context = mockk(relaxed = true)
        
        @BindValue @JvmField
        val mockCustomTabsIntentFactory: CustomTabsIntentFactory = mockk(relaxed = true)
        
        @Inject
        lateinit var viewModel: MainViewModel
        
        // インターフェースとしてViewModelを参照
        lateinit var viewModelContract: MainViewModelContract
        lateinit var testableViewModel: TestableViewModel<MainUiState>
        
        beforeTest {
            hiltRule.inject()
            viewModelContract = viewModel
            testableViewModel = viewModel
            
            // デフォルトのモック設定
            coEvery { mockAuthUseCase.isAuthenticated() } returns false
        }
        
        `when`("インターフェースベースのテスト") {
            then("ViewModelContractを通じて操作できる") {
                viewModelContract.clearError()
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModelContract.uiState.value.error shouldBe null
            }
            
            then("TestableViewModelを通じて状態を直接操作できる") {
                runTest {
                    viewModelContract.uiState.test {
                        // 初期状態を確認
                        awaitItem().apply {
                            isLoading shouldBe false
                            error shouldBe null
                        }
                        
                        // テスト用メソッドでエラー状態を設定
                        testableViewModel.setErrorState("テストエラー")
                        
                        awaitItem().apply {
                            error shouldBe "テストエラー"
                        }
                        
                        // ユーティリティメソッドでローディング状態を設定
                        testableViewModel.setLoadingState(true)
                        
                        awaitItem().apply {
                            isLoading shouldBe true
                        }
                        
                        // 状態をリセット
                        testableViewModel.resetToInitialState()
                        
                        awaitItem().apply {
                            isLoading shouldBe false
                            error shouldBe null
                        }
                    }
                }
            }
        }
        
        `when`("認証フローのテスト") {
            then("認証開始をインターフェース経由でテスト") {
                coEvery { mockAuthUseCase.getAuthUrl() } returns "https://example.com/auth"
                
                viewModelContract.startAuth()
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModelContract.uiState.value.isAuthenticating shouldBe true
                coVerify { mockAuthUseCase.getAuthUrl() }
            }
            
            then("認証コールバックをインターフェース経由でテスト") {
                coEvery { mockAuthUseCase.handleAuthCallback(any()) } returns true
                
                viewModelContract.handleAuthCallback("valid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                
                viewModelContract.uiState.value.isAuthenticated shouldBe true
                viewModelContract.uiState.value.isAuthenticating shouldBe false
                coVerify { mockAuthUseCase.handleAuthCallback("valid_code") }
            }
        }
        
        `when`("エラーハンドリングのテスト") {
            then("TestableViewModelで複雑な状態をシミュレート") {
                runTest {
                    viewModelContract.uiState.test {
                        // 初期状態をスキップ
                        awaitItem()
                        
                        // 複雑な状態をワンステップで設定
                        testableViewModel.setUiStateForTest(
                            MainUiState(
                                isLoading = false,
                                error = "ネットワークエラー",
                                isAuthenticating = false,
                                isAuthenticated = false
                            )
                        )
                        
                        awaitItem().apply {
                            error shouldBe "ネットワークエラー"
                            isAuthenticated shouldBe false
                        }
                        
                        // エラーをクリア
                        viewModelContract.clearError()
                        
                        awaitItem().apply {
                            error shouldBe null
                        }
                    }
                }
            }
        }
    }
})