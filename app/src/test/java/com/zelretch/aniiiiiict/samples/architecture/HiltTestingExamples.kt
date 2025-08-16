package com.zelretch.aniiiiiict.samples.architecture

/**
 * Hiltテスト機能を活用したViewModelテストの例
 * 
 * このファイルは実際のテストではなく、改善されたテストパターンの例を示すものです。
 * @BindValueと@UninstallModulesの効果的な使用方法をデモンストレーションします。
 */

/*
// Hiltテスト機能を活用したMainViewModelテストの例

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainViewModelHiltTest : BehaviorSpec({

    given("MainViewModel with Hilt testing features") {
        val hiltRule = HiltAndroidRule(this@MainViewModelHiltTest)
        
        // @BindValueを使用してテスト用の依存関係を注入
        // この方法により、実際のモジュールを置き換えることができる
        @BindValue @JvmField 
        val mockAuthUseCase: AnnictAuthUseCase = mockk(relaxUnitFun = true) {
            coEvery { isAuthenticated() } returns false
            coEvery { getAuthUrl() } returns "https://test.example.com/auth"
            coEvery { handleAuthCallback(any()) } returns true
        }
        
        @BindValue @JvmField
        val mockLogger: Logger = mockk(relaxed = true)
        
        @BindValue @JvmField
        val mockContext: Context = mockk(relaxed = true)
        
        @BindValue @JvmField
        val mockCustomTabsIntentFactory: CustomTabsIntentFactory = mockk(relaxed = true) {
            every { create() } returns mockk(relaxed = true)
        }
        
        // Hiltによって自動的に注入される
        @Inject
        lateinit var viewModel: MainViewModel
        
        // インターフェースとして参照
        lateinit var viewModelContract: MainViewModelContract
        lateinit var testableViewModel: TestableViewModel<MainUiState>
        
        beforeTest {
            hiltRule.inject()
            viewModelContract = viewModel
            testableViewModel = viewModel
        }
        
        `when`("Hiltによる依存関係注入のテスト") {
            then("モックされた依存関係が正しく注入される") {
                // ViewModelは@BindValueで指定したモックと共に作成される
                viewModelContract.startAuth()
                
                // モックの動作が呼び出されることを確認
                coVerify { mockAuthUseCase.getAuthUrl() }
                verify { mockLogger.info(any(), any(), any()) }
            }
            
            then("テスト用状態操作とHilt注入の組み合わせ") {
                // Hiltで注入されたViewModelに対してテスト用機能を使用
                testableViewModel.setErrorState("テストエラー")
                viewModelContract.uiState.value.error shouldBe "テストエラー"
                
                // 実際のメソッドも正常に動作
                viewModelContract.clearError()
                viewModelContract.uiState.value.error shouldBe null
            }
        }
        
        `when`("認証フローの統合テスト") {
            then("@BindValueによるモックで認証フロー全体をテスト") {
                // 認証開始
                viewModelContract.startAuth()
                viewModelContract.uiState.value.isAuthenticating shouldBe true
                
                // 認証コールバック
                viewModelContract.handleAuthCallback("test_code")
                viewModelContract.uiState.value.isAuthenticated shouldBe true
                
                // モックメソッドが期待通りに呼ばれることを確認
                coVerifySequence {
                    mockAuthUseCase.getAuthUrl()
                    mockAuthUseCase.handleAuthCallback("test_code")
                }
            }
        }
    }
})

// @UninstallModulesを使用した例
// プロダクションモジュールを削除してテスト専用モジュールを使用

@UninstallModules(
    // RepositoryModule::class,  // 実際のリポジトリモジュールを削除
    // UseCaseModule::class      // 実際のUseCaseモジュールを削除
)
@HiltAndroidTest
class MainViewModelWithTestModulesTest : BehaviorSpec({
    
    // テスト専用のモジュール
    @TestInstallIn(
        component = SingletonComponent::class,
        replaces = [/* RepositoryModule::class */]
    )
    @Module
    object TestRepositoryModule {
        @Provides
        @Singleton
        fun provideTestRepository(): /* Repository */ Any = mockk(relaxed = true)
    }
    
    given("Test modules with @UninstallModules") {
        `when`("テスト専用の実装を使用") {
            then("プロダクションコードとは異なる動作をテスト") {
                // テスト専用のリポジトリやUseCaseの動作を使用して
                // より制御されたテスト環境でViewModelをテスト可能
            }
        }
    }
})

*/

// 上記のコードは実際のテストコードの例です。
// このプロジェクトで実際に動作させるには、適切なテストモジュールの設定が必要です。

/**
 * Hiltテスト機能活用のメリット
 * 
 * 1. @BindValue: 簡単なモック注入
 *    - 個別の依存関係を簡単にモック化
 *    - テストごとに異なるモック動作を設定可能
 * 
 * 2. @UninstallModules + @TestInstallIn: 完全なモジュール置き換え
 *    - プロダクションモジュール全体をテスト用に置き換え
 *    - より大規模な統合テストに適している
 * 
 * 3. インターフェースベースのテストとの組み合わせ
 *    - Hiltで正しい依存関係を注入
 *    - インターフェース経由でテストの意図を明確化
 *    - TestableViewModelで必要に応じて状態を直接操作
 */