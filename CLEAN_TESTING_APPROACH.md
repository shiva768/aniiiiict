# プロダクションコードを汚染しないViewModelテストアプローチ

## 問題の解決

以前のアプローチでは、ViewModelのプロダクションコードにテスト専用のメソッドが含まれていました：

```kotlin
@HiltViewModel
class MainViewModel : MainViewModelContract, TestableViewModel<MainUiState> {
    // ❌ プロダクションコードにテスト専用メソッド
    override fun setUiStateForTest(state: MainUiState) { ... }
    override fun setErrorForTest(error: String?) { ... }
    override fun setLoadingForTest(isLoading: Boolean) { ... }
}
```

### 問題点

1. **プロダクションコードの肥大化** - テスト専用コードがプロダクションビルドに含まれる
2. **誤用のリスク** - 本番環境でテストメソッドが呼ばれる可能性
3. **責任の混在** - ViewModelがビジネスロジックとテスト支援の両方を担当
4. **ビルドサイズの増加** - 不要なコードがアプリサイズを増やす

## 新しい解決策

### 1. プロダクションコードの純化

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val annictAuthUseCase: AnnictAuthUseCase,
    private val customTabsIntentFactory: CustomTabsIntentFactory,
    logger: Logger,
    @ApplicationContext private val context: Context
) : BaseViewModel(logger), MainViewModelContract {
    // ✅ テスト専用コードは一切含まれない
    // ✅ ビジネスロジックにのみ集中
}
```

### 2. テスト専用拡張（app/src/test配下）

```kotlin
// app/src/test/java/com/zelretch/aniiiiiict/testing/ViewModelTestExtensions.kt

/**
 * テスト専用のViewModel拡張機能
 * プロダクションビルドには含まれない
 */
interface TestableMainViewModel {
    fun setUiStateForTest(state: MainUiState)
    fun setErrorForTest(error: String?)
    fun setLoadingForTest(isLoading: Boolean)
    fun resetToInitialState()
}

class MainViewModelTestWrapper(private val viewModel: MainViewModel) : TestableMainViewModel {
    // リフレクションを使用してプライベートフィールドにアクセス
    private val uiStateField: Field by lazy {
        MainViewModel::class.java.getDeclaredField("_uiState").apply {
            isAccessible = true
        }
    }
    
    // テスト専用実装...
}

// 拡張関数でテストコードを簡潔に
fun MainViewModel.asTestable(): TestableMainViewModel {
    return MainViewModelTestWrapper(this)
}
```

### 3. テストでの使用例

```kotlin
class MainViewModelTest : BehaviorSpec({
    
    given("MainViewModelのテスト") {
        
        `when`("状態操作が必要な場合") {
            then("拡張機能を使用して状態を設定できる") {
                val viewModel = MainViewModel(...)
                
                // プロダクション用インターフェース
                val contract: MainViewModelContract = viewModel
                
                // テスト専用機能（プロダクションビルドには含まれない）
                val testable: TestableMainViewModel = viewModel.asTestable()
                
                // 状態操作
                testable.setErrorForTest("テストエラー")
                testable.setLoadingForTest(true)
                
                // 検証
                contract.uiState.value.error shouldBe "テストエラー"
                contract.uiState.value.isLoading shouldBe true
            }
        }
        
        `when`("実装テストが必要な場合") {
            then("通常通りViewModelメソッドをテストできる") {
                val viewModel = MainViewModel(...)
                
                // ✅ 実際のビジネスロジックをテスト
                viewModel.startAuth()
                
                // ビジネスロジックの検証
                coVerify { authUseCase.getAuthUrl() }
            }
        }
    }
})
```

## メリット

### ✅ プロダクションコードの純度

- ViewModelクラスにはビジネスロジックのみ含まれる
- テスト専用コードは完全に分離される
- プロダクションビルドサイズの削減

### ✅ 安全性の向上

- テストメソッドが本番環境で呼ばれるリスクがゼロ
- プロダクションAPIとテストAPIが明確に分離

### ✅ テスト容易性の維持

- 状態操作は従来通り簡単
- インターフェースベースのテストもサポート
- 実装テストも変わらず可能

### ✅ 拡張性

- 他のViewModelでも同じパターンを適用可能
- 統一的なテストアプローチ

## 実装手順

1. **プロダクションコードからTestableViewModelの実装を削除**
   ```kotlin
   // 削除: TestableViewModel<MainUiState>の実装
   // 削除: setUiStateForTest, setErrorForTest, setLoadingForTestメソッド
   ```

2. **テスト専用拡張を作成**
   ```kotlin
   // app/src/test配下にViewModelTestExtensions.ktを作成
   ```

3. **既存テストを更新**
   ```kotlin
   // testableViewModel.setErrorState() → testableViewModel.setErrorForTest()
   // viewModel as TestableViewModel → viewModel.asTestable()
   ```

## 他のViewModelでの適用

同じパターンをTrackViewModel、HistoryViewModelなどでも使用可能：

```kotlin
// TrackViewModel用
fun TrackViewModel.asTestable(): TestableTrackViewModel = TrackViewModelTestWrapper(this)

// HistoryViewModel用  
fun HistoryViewModel.asTestable(): TestableHistoryViewModel = HistoryViewModelTestWrapper(this)
```

## まとめ

この新しいアプローチにより：

- **プロダクションコードは純粋で焦点が明確**
- **テスト容易性は完全に維持**
- **誤用リスクが完全に排除**
- **ビルドサイズが最適化**

プロダクションコードの品質を保ちながら、テストの記述しやすさを両立する理想的な解決策です。