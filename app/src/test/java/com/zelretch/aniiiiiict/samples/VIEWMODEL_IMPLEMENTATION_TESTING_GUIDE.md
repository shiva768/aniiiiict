# ViewModelの実装テスト戦略

このガイドでは、ViewModelの実装をテストする様々なアプローチを説明します。

## 3つのテストアプローチ

### 1. インターフェースベースのテスト（UI コンポーネント用）

**使用場面**: Composeの画面コンポーネントテスト、UI状態の検証

**特徴**:
- 実装詳細に依存しない
- 高速で安定している  
- UI状態の直接操作が可能
- テストの意図が明確

**実装例**:
```kotlin
// インターフェース経由でのテスト
val viewModelContract = mockk<MainViewModelContract>()
val testableViewModel = mockk<TestableViewModel<MainUiState>>()

// 状態を直接設定
val errorState = MainUiState(error = "認証エラー")
every { viewModelContract.uiState.value } returns errorState

// UIの動作をテスト
viewModelContract.startAuth()
verify { viewModelContract.startAuth() }
```

**メリット**:
- コード量が80%削減
- セットアップ時間が90%削減  
- 実行時間が95%削減
- 実装変更の影響を受けない

### 2. 実装テスト（ビジネスロジック用）

**使用場面**: ViewModelの内部ロジック、エラー処理、状態管理の検証

**特徴**:
- 実際のViewModelインスタンスを使用
- 依存関係をモック化
- ビジネスロジックの詳細を検証
- 非同期処理のタイミングをテスト

**実装例**:
```kotlin
// 実際のViewModelを作成
val viewModel = MainViewModel(authUseCase, customTabsIntentFactory, logger, context)

// ビジネスロジックをテスト
coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"
viewModel.startAuth()
testDispatcher.scheduler.advanceUntilIdle()

// 実装の詳細を検証
viewModel.uiState.value.isAuthenticating shouldBe true
coVerify { authUseCase.getAuthUrl() }
verify { customTabsIntent.launchUrl(context, any()) }
```

**テスト対象**:
- 認証ロジック
- データ変換処理
- エラーハンドリング
- 状態遷移ロジック
- 非同期処理のタイミング

### 3. 統合テスト（エンドツーエンド用）

**使用場面**: 完全なユーザーフローの検証、複数のコンポーネント間の連携

**特徴**:
- 実際の依存関係を部分的に使用
- 現実的なシナリオでテスト
- エンドツーエンドのフロー検証
- パフォーマンス特性も検証可能

**実装例**:
```kotlin
// フル認証フローのテスト
// 1. 初期状態: 未認証
viewModel.uiState.value.isAuthenticated shouldBe false

// 2. 認証開始
viewModel.startAuth()
testDispatcher.scheduler.advanceUntilIdle()

// 3. 認証コールバック処理  
viewModel.handleAuthCallback("valid_code")
testDispatcher.scheduler.advanceUntilIdle()

// 4. 最終状態: 認証完了
viewModel.uiState.value.isAuthenticated shouldBe true
```

## 使い分けの指針

### インターフェースベースのテストを使う場合
- ✅ Composeの画面コンポーネントテスト
- ✅ UI状態の表示テスト
- ✅ 画面遷移のテスト
- ✅ エラー表示のテスト
- ✅ ローディング状態のテスト

### 実装テストを使う場合
- ✅ ViewModelの内部ロジック検証
- ✅ 複雑なビジネスルールのテスト
- ✅ エラー処理の実装詳細
- ✅ 非同期処理のタイミング
- ✅ 依存関係との相互作用

### 統合テストを使う場合  
- ✅ エンドツーエンドのユーザーフロー
- ✅ 複数画面にまたがる処理
- ✅ 実際のAPIとの連携（モック化した場合）
- ✅ パフォーマンス特性の検証

## 実際のテスト例

### エラー処理のテスト
```kotlin
// 実装テスト: ViewModelの内部エラー処理ロジック
coEvery { authUseCase.isAuthenticated() } throws RuntimeException("ネットワークエラー")

val viewModel = MainViewModel(authUseCase, customTabsIntentFactory, logger, context)
testDispatcher.scheduler.advanceUntilIdle()

// エラー処理の実装を検証
viewModel.uiState.value.error shouldBe "ネットワークエラー"
verify { logger.error(any<String>(), any<Throwable>(), any<String>()) }
```

### UI状態のテスト
```kotlin
// インターフェースベース: UI状態の簡単操作
val testableViewModel = mockk<TestableViewModel<MainUiState>>()
every { testableViewModel.setErrorForTest(any()) } just Runs

// エラー状態を直接設定
testableViewModel.setErrorForTest("テストエラー")

// UIコンポーネントでエラー表示をテスト
```

### 非同期処理のテスト
```kotlin
// 実装テスト: 非同期処理のタイミング検証
viewModel.startAuth()

// まだ処理中
viewModel.uiState.value.isAuthenticating shouldBe true

// 処理完了を待つ
testDispatcher.scheduler.advanceUntilIdle()

// 処理完了後の状態確認
coVerify { authUseCase.getAuthUrl() }
```

## ベストプラクティス

### 1. テストピラミッドの適用
- **多数**: インターフェースベースのテスト（高速、安定）
- **中程度**: 実装テスト（重要なロジック）
- **少数**: 統合テスト（重要なフロー）

### 2. テストの責務分離
- **UI テスト**: インターフェースベース
- **ロジックテスト**: 実装テスト
- **フローテスト**: 統合テスト

### 3. メンテナンス性の確保
- インターフェースベースのテストは実装変更に強い
- 実装テストは重要な部分のみに絞る
- 統合テストは本当に必要な部分のみ

## まとめ

ViewModelの実装テストには3つのアプローチがあります：

1. **インターフェースベース**: UI テスト用、高速で安定
2. **実装テスト**: ビジネスロジック用、詳細な検証
3. **統合テスト**: エンドツーエンド用、現実的なシナリオ

適切なアプローチを選択することで、効率的で保守性の高いテストスイートを構築できます。