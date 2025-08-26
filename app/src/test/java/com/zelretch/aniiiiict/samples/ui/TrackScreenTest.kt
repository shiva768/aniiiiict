package com.zelretch.aniiiiict.samples.ui

import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.ui.track.TrackUiState
import com.zelretch.aniiiiict.ui.track.TrackViewModelContract
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * TrackScreen用ViewModel状態管理のテスト
 * ViewModelのインターフェースを使用することで、テスト容易性を向上させた例
 *
 * このテストでは、実際のUI描画はテストせず、ViewModelの契約とTestableViewModelの
 * 機能を活用したロジックテストに焦点を当てる
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackScreenTest : BehaviorSpec({

    lateinit var testDispatcher: TestDispatcher

    beforeTest {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("TrackScreenのViewModel契約テスト") {

        `when`("インターフェースベースでテストを作成する") {
            then("ViewModelContractを通じて状態にアクセスできる") {
                // インターフェースのモックを作成
                val viewModelContract = mockk<TrackViewModelContract>()

                // 初期状態を定義し、モックに設定
                val initialState = TrackUiState()
                every { viewModelContract.uiState } returns MutableStateFlow(initialState)

                // 契約経由で状態取得ができることを確認
                val uiState = viewModelContract.uiState.value
                uiState.programs.size shouldBe 0
                uiState.isLoading shouldBe false
                uiState.error shouldBe null
            }
        }

        `when`("TestableViewModelで直接状態を操作する (デモ)") {
            then("エラー状態を簡単に設定できる") {
                val viewModelContract = mockk<TrackViewModelContract>()

                // エラー状態を直接設定
                val errorState = TrackUiState(error = "ネットワークエラーが発生しました")
                every { viewModelContract.uiState } returns MutableStateFlow(errorState)

                // エラー状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.error shouldNotBe null
                currentState.error shouldBe "ネットワークエラーが発生しました"
            }

            then("ローディング状態をユーティリティ関数で設定できる") {
                val viewModelContract = mockk<TrackViewModelContract>()

                // ローディング状態を直接設定
                val loadingState = TrackUiState(isLoading = true)
                every { viewModelContract.uiState } returns MutableStateFlow(loadingState)

                // ローディング状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.isLoading shouldBe true
            }
        }

        `when`("契約メソッドの呼び出しをテストする") {
            then("フィルター切り替えメソッドが正しく呼ばれる") {
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)

                // フィルター切り替えアクション
                viewModelContract.toggleFilterVisibility()

                // 契約で定義されたメソッドが呼ばれることを確認
                verify { viewModelContract.toggleFilterVisibility() }
            }
        }

        `when`("複雑な状態の組み合わせをテストする") {
            then("フィルター表示とプログラム一覧を同時にテストできる") {
                val viewModelContract = mockk<TrackViewModelContract>()

                // サンプルプログラム作成
                val sampleWork = Work(
                    id = "1",
                    title = "テストアニメ",
                    seasonName = com.annict.type.SeasonName.SPRING,
                    seasonYear = 2024,
                    media = "TV",
                    mediaText = "TV",
                    viewerStatusState = com.annict.type.StatusState.WATCHING,
                    seasonNameText = "2024春",
                    image = null
                )
                val sampleProgram = ProgramWithWork(
                    programs = listOf(mockk(relaxed = true)),
                    firstProgram = mockk(relaxed = true),
                    work = sampleWork
                )

                // 複合状態を設定：フィルター表示 + プログラム一覧
                val complexState = TrackUiState(
                    isFilterVisible = true,
                    programs = listOf(sampleProgram),
                    availableMedia = listOf("TV", "Movie"),
                    availableSeasons = listOf(com.annict.type.SeasonName.SPRING, com.annict.type.SeasonName.SUMMER),
                    availableYears = listOf(2024, 2023)
                )
                every { viewModelContract.uiState } returns MutableStateFlow(complexState)

                // 複合状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.isFilterVisible shouldBe true
                currentState.programs.size shouldBe 1
                currentState.programs.first().work.title shouldBe "テストアニメ"
                currentState.availableMedia.size shouldBe 2
            }
        }

        `when`("最終話確認ダイアログの状態をテストする") {
            then("複雑な状態も直接設定してテストできる") {
                val viewModelContract = mockk<TrackViewModelContract>()

                // 最終話確認状態を直接設定
                val finaleState = TrackUiState(
                    showFinaleConfirmationForWorkId = "work123",
                    showFinaleConfirmationForEpisodeNumber = 12
                )
                every { viewModelContract.uiState } returns MutableStateFlow(finaleState)

                // 最終話確認状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.showFinaleConfirmationForWorkId shouldBe "work123"
                currentState.showFinaleConfirmationForEpisodeNumber shouldBe 12
            }
        }
    }
})
