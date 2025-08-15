# ViewModelテスト容易性向上ガイド

このドキュメントでは、ViewModelのテスト容易性を向上させるために実装されたリファクタリング内容と、新しいテスト手法について説明します。

## 課題と解決策

### 課題 1: @HiltViewModelクラスが final であるため継承によるテストダブルの作成が困難

**解決策: インターフェースベースのアプローチ**

各ViewModelに対応するインターフェースを導入し、テストでは実装ではなくインターフェースに依存するようにしました。

```kotlin
// ViewModelの基本契約
interface ViewModelContract<T : BaseUiState> {
    val uiState: StateFlow<T>
    fun clearError()
}

// MainViewModel専用の契約
interface MainViewModelContract : ViewModelContract<MainUiState> {
    fun startAuth()
    fun handleAuthCallback(code: String?)
    fun checkAuthentication()
}

// 実装
@HiltViewModel
class MainViewModel @Inject constructor(
    // ... dependencies
) : BaseViewModel(logger), MainViewModelContract, TestableViewModel<MainUiState> {
    // ... implementation
}
```

### 課題 2: uiState プロパティが final であるためテスト時の状態操作が困難

**解決策: テスト専用インターフェースとユーティリティの導入**

```kotlin
// テスト時のUI状態操作を可能にするインターフェース
interface TestableViewModel<T : BaseUiState> {
    fun setUiStateForTest(state: T)
    fun setErrorForTest(error: String?)
    fun setLoadingForTest(isLoading: Boolean)
}

// テスト用ユーティリティ
object ViewModelTestUtils {
    inline fun <reified T : BaseUiState> TestableViewModel<T>.setErrorState(error: String) {
        setErrorForTest(error)
    }
    
    inline fun <reified T : BaseUiState> TestableViewModel<T>.resetToInitialState() {
        setLoadingForTest(false)
        setErrorForTest(null)
    }
}
```

## 新しいテスト手法

### 1. インターフェースベースのテスト

ViewModelを具象クラスではなくインターフェースとして参照することで、テストの意図を明確にし、将来的な実装変更に対する耐性を向上させます。

```kotlin
class MainViewModelImprovedTest : BehaviorSpec({
    lateinit var viewModelContract: MainViewModelContract
    lateinit var testableViewModel: TestableViewModel<MainUiState>
    
    beforeTest {
        val viewModel = MainViewModel(/* dependencies */)
        viewModelContract = viewModel  // インターフェースとして参照
        testableViewModel = viewModel   // テスト機能として参照
    }
    
    `when`("認証テスト") {
        then("インターフェース経由で認証開始") {
            viewModelContract.startAuth()
            // アサーション
        }
    }
})
```

### 2. Hiltテスト機能の活用

`@BindValue`を使用して、テスト用の依存関係を簡単に注入できます。

```kotlin
@HiltAndroidTest
class MainViewModelImprovedTest : BehaviorSpec({
    @BindValue @JvmField 
    val mockAuthUseCase: AnnictAuthUseCase = mockk(relaxUnitFun = true)
    
    @Inject
    lateinit var viewModel: MainViewModel
    
    beforeTest {
        hiltRule.inject()
        // ViewModelは自動的に正しい依存関係で構築される
    }
})
```

### 3. 直接的な状態操作によるテストシナリオ作成

複雑なUI状態を直接設定することで、特定のシナリオを効率的にテストできます。

```kotlin
`when`("エラーハンドリングテスト") {
    then("複雑な状態をワンステップで設定") {
        testableViewModel.setUiStateForTest(
            MainUiState(
                isLoading = false,
                error = "ネットワークエラー",
                isAuthenticating = false,
                isAuthenticated = false
            )
        )
        
        // 設定した状態からのテストを開始
        viewModelContract.clearError()
        // アサーション
    }
}
```

### 4. テスト用ユーティリティによる簡潔なテスト

```kotlin
// ユーティリティメソッドで状態操作を簡潔に
testableViewModel.setErrorState("テストエラー")
testableViewModel.setLoadingState(true)
testableViewModel.resetToInitialState()
```

## 実装例

### TrackViewModelの改善

```kotlin
// インターフェース定義
interface TrackViewModelContract : ViewModelContract<TrackUiState> {
    fun watchEpisode(program: ProgramWithWork, episodeNumber: Int)
    fun toggleFilterVisibility()
    fun showDetailModal(program: ProgramWithWork)
    // ...
}

// テスト専用インターフェース
interface TestableTrackViewModel {
    var externalScope: CoroutineScope?
    fun setUiStateForTest(state: TrackUiState)
}

// 実装
@HiltViewModel
class TrackViewModel @Inject constructor(
    // ... dependencies
) : BaseViewModel(logger), TrackViewModelContract, TestableTrackViewModel {
    
    // === 既存の実装 ===
    
    // === インターフェース実装 ===
    override fun watchEpisode(program: ProgramWithWork, episodeNumber: Int) {
        val episode = program.programs.find { it.episode.number == episodeNumber }
        episode?.let {
            recordEpisode(it.episode.id, program.work.id, program.work.viewerStatusState)
        }
    }
    
    // === テスト用実装 ===
    override fun setUiStateForTest(state: TrackUiState) {
        _uiState.value = state
    }
}
```

## メリット

1. **テストの意図明確化**: インターフェースベースのテストにより、何をテストしているかが明確
2. **実装詳細からの分離**: ViewModelの内部実装変更がテストに与える影響を最小限に
3. **複雑なシナリオの簡潔な記述**: 直接的な状態操作により、セットアップコードを削減
4. **Hiltテスト機能の活用**: `@BindValue`などによる依存関係注入の簡素化
5. **保守性の向上**: 一貫したテストパターンによる保守性の向上

## 注意事項

- `TestableViewModel`のメソッドは**テスト時のみ**使用し、プロダクションコードでは使用しない
- インターフェースの変更は互換性を考慮して慎重に行う
- テスト用のメソッドはプロダクションバイナリサイズに影響するため、必要最小限に留める

## マイグレーション

既存のテストコードは段階的に新しいパターンに移行することを推奨します：

1. まずインターフェースベースの参照に変更
2. 複雑なセットアップが必要な部分で`TestableViewModel`を活用
3. Hiltテストの活用は既存のモックベースから段階的に移行