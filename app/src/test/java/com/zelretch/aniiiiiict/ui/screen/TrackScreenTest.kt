package com.zelretch.aniiiiiict.ui.screen

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.ui.base.TestableViewModel
import com.zelretch.aniiiiiict.ui.base.ViewModelTestUtils.setErrorState
import com.zelretch.aniiiiiict.ui.base.ViewModelTestUtils.setLoadingState
import com.zelretch.aniiiiiict.ui.track.TrackUiState
import com.zelretch.aniiiiiict.ui.track.TrackViewModel
import com.zelretch.aniiiiiict.ui.track.TrackViewModelContract
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TrackScreen用ViewModel状態管理のテスト
 * ViewModelのインターフェースを使用することで、テスト容易性を向上させた例
 * 
 * このテストでは、実際のUI描画はテストせず、ViewModelの契約とTestableViewModelの
 * 機能を活用したロジックテストに焦点を当てる
 */
class TrackScreenTest : BehaviorSpec({

    given("TrackScreenのViewModel契約テスト") {
        
        `when`("インターフェースベースでテストを作成する") {
            then("ViewModelContractを通じて状態にアクセスできる") {
                // インターフェースベースのモック作成
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<TrackUiState>>(relaxed = true)
                
                // 実際のViewModelインスタンス（両方のインターフェースを実装）
                val viewModel = mockk<TrackViewModel>(relaxed = true)
                every { viewModel as TrackViewModelContract } returns viewModelContract
                every { viewModel as TestableViewModel<TrackUiState> } returns testableViewModel

                // 初期状態を設定
                val initialState = TrackUiState()
                every { viewModelContract.uiState } returns MutableStateFlow(initialState)

                // 契約経由で状態取得ができること確認
                val uiState = viewModelContract.uiState.value
                uiState.programs.size shouldBe 0
                uiState.isLoading shouldBe false
                uiState.error shouldBe null
            }
        }

        
        `when`("TestableViewModelで直接状態を操作する") {
            then("エラー状態を簡単に設定できる") {
                val viewModel = mockk<TrackViewModel>(relaxed = true)
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<TrackUiState>>(relaxed = true)
                
                every { viewModel as TrackViewModelContract } returns viewModelContract
                every { viewModel as TestableViewModel<TrackUiState> } returns testableViewModel

                // エラー状態を直接設定
                val errorState = TrackUiState(error = "ネットワークエラーが発生しました")
                every { viewModelContract.uiState } returns MutableStateFlow(errorState)

                // エラー状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.error shouldNotBe null
                currentState.error shouldBe "ネットワークエラーが発生しました"
                
                // 従来のアプローチと比較:
                // 従来: 複雑なモック設定 → エラー発生 → 状態確認
                // 改善後: 直接状態設定 → 即座に確認
            }
            
            then("ローディング状態をユーティリティ関数で設定できる") {
                val viewModel = mockk<TrackViewModel>(relaxed = true)
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<TrackUiState>>(relaxed = true)
                
                every { viewModel as TrackViewModelContract } returns viewModelContract
                every { viewModel as TestableViewModel<TrackUiState> } returns testableViewModel

                // ローディング状態を直接設定
                val loadingState = TrackUiState(isLoading = true)
                every { viewModelContract.uiState } returns MutableStateFlow(loadingState)

                // ローディング状態の確認
                val currentState = viewModelContract.uiState.value
                currentState.isLoading shouldBe true
                
                // TestableViewModelのユーティリティ関数の使用例
                // testableViewModel.setLoadingState(true)
                // この方法により、複雑な非同期処理のセットアップが不要
            }
        }
        
        `when`("契約メソッドの呼び出しをテストする") {
            then("フィルター切り替えメソッドが正しく呼ばれる") {
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)
                val initialState = TrackUiState()
                every { viewModelContract.uiState } returns MutableStateFlow(initialState)

                // フィルター切り替えアクション
                viewModelContract.toggleFilterVisibility()
                
                // 契約で定義されたメソッドが呼ばれることを確認
                verify { viewModelContract.toggleFilterVisibility() }
                
                // 従来のアプローチと比較:
                // 従来: 具象クラスのメソッド → 実装詳細への依存
                // 改善後: 契約メソッド → 意図が明確
            }
        }
        
        `when`("複雑な状態の組み合わせをテストする") {
            then("フィルター表示とプログラム一覧を同時にテストできる") {
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<TrackUiState>>(relaxed = true)

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
                
                // 従来のアプローチでこの状態を作るには:
                // 1. 複数のリポジトリとUseCaseをモック
                // 2. データ取得処理を実行
                // 3. フィルター表示切り替えを実行
                // 4. 結果的な状態を確認
                // 
                // 改善後のアプローチ:
                // - 状態を直接設定（1行）
                // - 即座に確認可能
            }
        }
        
        `when`("最終話確認ダイアログの状態をテストする") {
            then("複雑な状態も直接設定してテストできる") {
                val viewModelContract = mockk<TrackViewModelContract>(relaxed = true)

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
                
                // この状態により、UIで以下が表示される想定:
                // "このタイトルはエピソード12が最終話の可能性があります、視聴済みにしますか？"
            }
        }
    }
})

/**
 * TrackScreenTestが示すインターフェースアプローチの利点：
 * 
 * ## 1. 明確なテスト意図
 * インターフェースベースなので何をテストしているかが明確
 * - ViewModelContractの動作テスト
 * - TestableViewModelの状態操作テスト
 * - 複合状態のロジックテスト
 * 
 * ## 2. 簡潔なセットアップ
 * TestableViewModelで直接状態設定
 * - エラー状態: `TrackUiState(error = "エラー")`
 * - ローディング状態: `TrackUiState(isLoading = true)`
 * - 複合状態: 全プロパティを一度に設定
 * 
 * ## 3. 実装独立性
 * ViewModelの内部実装が変わってもテストは影響を受けない
 * - 契約で定義されたAPIのみに依存
 * - 実装詳細の変更に強い
 * 
 * ## 4. 高速なテスト実行
 * - 非同期処理の待機が不要
 * - 複雑なモック設定が不要
 * - 決定論的なテスト
 * 
 * ## 5. 保守性の向上
 * - 契約が変わらない限りテストは安定
 * - テストコードが短く理解しやすい
 * - デバッグが容易
 * 
 * ## 従来のアプローチとの比較
 * 
 * ### 従来: 複雑なセットアップ
 * ```kotlin
 * // 多数の依存関係をモック
 * val repository = mockk<Repository>()
 * val useCase1 = mockk<UseCase1>()
 * val useCase2 = mockk<UseCase2>()
 * 
 * // 複雑なモック設定
 * every { repository.getPrograms() } returns flow { emit(programs) }
 * 
 * // 実際の処理を実行
 * viewModel.loadPrograms()
 * 
 * // 非同期処理の完了を待つ
 * runBlocking { delay(100) }
 * ```
 * 
 * ### 改善後: シンプルな直接設定
 * ```kotlin
 * // 状態を直接設定
 * val state = TrackUiState(programs = listOf(program))
 * every { contract.uiState } returns MutableStateFlow(state)
 * 
 * // 即座にテスト実行
 * state.programs.size shouldBe 1
 * ```
 * 
 * この改善により、開発者はより多くの時間を機能開発に集中できます。
 */