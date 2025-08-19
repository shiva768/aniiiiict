package com.zelretch.aniiiiiict.samples.architecture

import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiiict.testing.MainUiStateBuilder
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CleanTestingApproachDemoTest : BehaviorSpec({

    lateinit var testDispatcher: TestDispatcher

    beforeTest {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("プロダクションコードを汚染しないテストアプローチ") {

        `when`("新しい改善されたアプローチ") {
            then("プロダクションコードは純粋でテスト機能は分離されている") {
                val mockAuthUseCase = mockk<AnnictAuthUseCase>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<CustomTabsIntentFactory>(relaxed = true)

                coEvery { mockAuthUseCase.isAuthenticated() } returns false

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockContext
                )

                // initブロック内の処理を待機
                testDispatcher.scheduler.runCurrent()

                val contract: MainViewModelContract = viewModel

                // 初期状態を確認
                contract.uiState.value.isAuthenticated shouldBe false
                contract.clearError()

                // `internal`フィールドに直接アクセスして状態を操作
                viewModel._uiState.value = MainUiStateBuilder.authenticated()
                contract.uiState.value.isAuthenticated shouldBe true

                viewModel._uiState.value = MainUiStateBuilder.error("テストエラー")
                contract.uiState.value.error shouldBe "テストエラー"

                viewModel._uiState.value = MainUiStateBuilder.custom() // 初期状態に戻す
                contract.uiState.value.error shouldBe null
                contract.uiState.value.isAuthenticated shouldBe false
            }
        }

        `when`("メリットの比較") {
            then("実装テストも変わらずサポートされる") {
                val mockAuthUseCase = mockk<AnnictAuthUseCase>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<CustomTabsIntentFactory>(relaxed = true)

                coEvery { mockAuthUseCase.getAuthUrl() } returns "http://example.com/auth"

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockContext
                )

                viewModel.startAuth()

                coVerify { mockAuthUseCase.getAuthUrl() }
            }
        }
    }
})
