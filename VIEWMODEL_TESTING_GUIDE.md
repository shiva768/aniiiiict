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
) : BaseViewModel(), MainViewModelContract, TestableViewModel<MainUiState> {
    // ... implementation
    // ✅ Timberを直接使用してログ出力（Logger interfaceは不要）
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

## 実装されたファイル

### 新規作成したファイル

1. **`ViewModelContract.kt`** - 基本的なViewModel契約とテスト用インターフェース
2. **`MainViewModelContract.kt`** - MainViewModel専用のインターフェース
3. **`TrackViewModelContract.kt`** - TrackViewModel専用のインターフェース
4. **`ViewModelTestUtils.kt`** - テスト用のユーティリティ関数
5. **`ViewModelTestabilityDemoTest.kt`** - 改善されたテスト容易性のデモンストレーション
6. **`HiltTestingExamples.kt`** - Hiltテスト機能の活用例（コメント形式）

### 更新したファイル

1. **`MainViewModel.kt`** - インターフェースの実装とテスト用メソッドの追加
2. **`TrackViewModel.kt`** - インターフェースの実装とテスト用メソッドの追加

## 新しいテスト手法

### 1. インターフェースベースのテスト

ViewModelを具象クラスではなくインターフェースとして参照することで、テストの意図を明確にし、将来的な実装変更に対する耐性を向上させます。

```kotlin
// 従来のアプローチ
val viewModel = MainViewModel(dependencies...)
viewModel.startAuth() // 具象クラスに依存

// 改善されたアプローチ
val viewModel = MainViewModel(dependencies...)
val viewModelContract: MainViewModelContract = viewModel // インターフェースとして参照
viewModelContract.startAuth() // インターフェースの契約に依存
```

### 2. 直接的な状態操作によるテストシナリオ作成

複雑なUI状態を直接設定することで、特定のシナリオを効率的にテストできます。

```kotlin
val testableViewModel: TestableViewModel<MainUiState> = viewModel

// 複雑な状態をワンステップで設定
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
```

### 3. テスト用ユーティリティによる簡潔なテスト

```kotlin
// ユーティリティメソッドで状態操作を簡潔に
testableViewModel.setErrorState("テストエラー")
testableViewModel.setLoadingState(true)
testableViewModel.resetToInitialState()
```

## デモンストレーション

`ViewModelTestabilityDemoTest.kt` では以下の改善点をデモンストレーションしています：

1. **インターフェースベースのアクセス**: ViewModelをインターフェース経由で操作
2. **直接的な状態設定**: 複雑な状態をワンステップで作成
3. **ユーティリティメソッドの活用**: 簡潔な状態操作
4. **従来手法との比較**: セットアップの簡素化効果

## Hiltテスト機能の活用（将来実装予定）

`HiltTestingExamples.kt` で以下のパターンを例示しています：

### 1. @BindValue による簡単なモック注入

```kotlin
@BindValue @JvmField 
val mockAuthUseCase: AnnictAuthUseCase = mockk(relaxUnitFun = true)

@Inject
lateinit var viewModel: MainViewModel
```

### 2. @UninstallModules による完全なモジュール置き換え

```kotlin
@UninstallModules(RepositoryModule::class)
@TestInstallIn(component = SingletonComponent::class, replaces = [RepositoryModule::class])
@Module
object TestRepositoryModule {
    @Provides
    fun provideTestRepository(): Repository = mockk(relaxed = true)
}
```

## メリット

1. **テストの意図明確化**: インターフェースベースのテストにより、何をテストしているかが明確
2. **実装詳細からの分離**: ViewModelの内部実装変更がテストに与える影響を最小限に
3. **複雑なシナリオの簡潔な記述**: 直接的な状態操作により、セットアップコードを削減
4. **保守性の向上**: 一貫したテストパターンによる保守性の向上
5. **拡張性**: 新しいViewModelでも同じパターンを適用可能

## 注意事項

- `TestableViewModel`のメソッドは**テスト時のみ**使用し、プロダクションコードでは使用しない
- インターフェースの変更は互換性を考慮して慎重に行う
- テスト用のメソッドはプロダクションバイナリサイズに影響するため、必要最小限に留める

## マイグレーション

既存のテストコードは段階的に新しいパターンに移行することを推奨します：

1. まずインターフェースベースの参照に変更
2. 複雑なセットアップが必要な部分で`TestableViewModel`を活用
3. 新しいViewModelではこのパターンを最初から適用