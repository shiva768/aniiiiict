# Compose UI Tests

このプロジェクトでは、主要な画面（TrackScreen、HistoryScreen、DetailModal、AuthScreen）に対してCompose UIテストを導入しています。
各画面では、UITestとIntegrationTestの2種類のテストに分割して実装されています。

## テストアプローチ

### UITest vs IntegrationTest

各画面では2つの異なるアプローチでテストを実装しています：

**UITest (*UITest.kt)**
- ViewModelを完全にモック化
- 特定のUI状態が与えられた際のUIの描画とインタラクションのみを検証
- UI層のみに焦点を当てた純粋なテスト
- 高速で安定したテスト実行
- `UiState<T>`パターンを使用する画面では、3つの状態すべてを検証:
  - `UiState.Loading`: ローディング表示の確認
  - `UiState.Success<T>`: データが正しく表示されることの確認
  - `UiState.Error`: エラーメッセージ表示の確認

**IntegrationTest (*IntegrationTest.kt)**
- UI操作からViewModel、UseCaseを経由してRepository（モック）まで
- コンポーネント間の連携を検証する統合テスト
- より実際のユーザー操作に近いテストシナリオ
- ビジネスロジックとUI の結合をテスト
- `ErrorMapper`を含めたエラーハンドリングフロー全体を検証

## テストファイルの場所

```
app/src/androidTest/java/com/zelretch/aniiiiict/ui/
├── track/TrackScreenUITest.kt
├── track/TrackScreenIntegrationTest.kt
├── history/HistoryScreenUITest.kt
├── history/HistoryScreenIntegrationTest.kt
├── details/DetailModalUITest.kt
├── details/DetailModalIntegrationTest.kt
├── auth/AuthScreenUITest.kt
└── auth/AuthScreenIntegrationTest.kt
```

## 実装されたテスト

### TrackScreenのテスト

#### UITest (TrackScreenUITest.kt)
ViewModelをモック化し、特定のUI状態が与えられた際のUIの描画とインタラクションを検証する。

#### IntegrationTest (TrackScreenIntegrationTest.kt)  
UI操作からViewModel、UseCaseを経由し、Repository（モック）が正しく呼び出されるかという、コンポーネント間の連携を検証する。

#### テスト項目

- `trackScreen_初期状態_基本要素が表示される()` - 基本的なUI要素の表示確認
- `trackScreen_エラー状態_スナックバーとエラーメッセージが表示される()` - エラー状態のUI検証
- `trackScreen_フィルターボタンクリック_ViewModelメソッドが呼ばれる()` - インタラクションテスト
- `trackScreen_番組リスト_プログラムカードが表示される()` - データ表示テスト
- `trackScreen_フィナーレ確認_適切なスナックバーが表示される()` - 特殊状態のUI検証
- `trackScreen_フィナーレ確認_はいボタンクリック()` - 確認ダイアログのインタラクション
- `trackScreen_履歴ナビゲーション_コールバックが呼ばれる()` - ナビゲーションテスト

### HistoryScreenのテスト

#### UITest (HistoryScreenUITest.kt)
ViewModelをモック化し、履歴画面のUI状態とインタラクションを検証する。

#### IntegrationTest (HistoryScreenIntegrationTest.kt)
履歴機能のUI操作からRepository呼び出しまでの統合的な動作を検証する。

#### テスト項目

- `historyScreen_初期状態_基本要素が表示される()` - 基本的なUI要素の表示確認
- `historyScreen_空の状態_適切なメッセージが表示される()` - 空状態のメッセージ表示
- `historyScreen_履歴データ_レコードが表示される()` - データ表示テスト
- `historyScreen_戻るボタンクリック_ナビゲーションコールバックが呼ばれる()` - ナビゲーション
- `historyScreen_検索入力_コールバックが呼ばれる()` - 検索機能テスト
- `historyScreen_検索文字入力済み_クリアボタンが表示される()` - 検索状態のUI検証
- `historyScreen_クリアボタンクリック_検索文字がクリアされる()` - 検索クリア機能
- `historyScreen_削除ボタンクリック_削除コールバックが呼ばれる()` - 削除機能テスト
- `historyScreen_エラー状態_エラーメッセージと再試行ボタンが表示される()` - エラー状態のUI検証
- `historyScreen_再試行ボタンクリック_再試行コールバックが呼ばれる()` - エラー回復機能
- `historyScreen_次のページあり_もっと見るボタンが表示される()` - ページネーション表示
- `historyScreen_もっと見るボタンクリック_次ページ読み込みコールバックが呼ばれる()` - ページネーション機能

### DetailModalのテスト

#### UITest (DetailModalUITest.kt)
ViewModelをモック化し、詳細モーダルのUI状態とインタラクションを検証する。

#### IntegrationTest (DetailModalIntegrationTest.kt)
詳細モーダルのUI操作からRepository呼び出しまでの統合的な動作を検証する。

#### テスト項目

- `detailModal_基本要素_タイトルと閉じるボタンが表示される()` - 基本的なUI要素の表示確認
- `detailModal_ステータスドロップダウン_展開して選択できる()` - ドロップダウン機能テスト
- `detailModal_一括視聴確認ダイアログ_表示内容が正しい()` - 確認ダイアログのUI検証
- `detailModal_一括視聴確認_確認でRepository呼び出しをcoVerifyできる()` - 統合テスト

### AuthScreenのテスト

#### UITest (AuthScreenUITest.kt)
認証画面のUI状態とインタラクションを検証する。

#### IntegrationTest (AuthScreenIntegrationTest.kt)
認証機能のUI操作からRepository呼び出しまでの統合的な動作を検証する。

#### テスト項目

- `authScreen_未認証状態_ログインボタンが表示される()` - 基本的なUI要素の表示確認
- `authScreen_ログインボタンクリック_onLoginClickが呼ばれる()` - インタラクションテスト
- `authScreen_ログインボタンクリック_getAuthUrlが呼ばれる()` - 統合テスト
- `authScreen_コールバック処理_handleAuthCallbackが呼ばれる()` - 認証フロー統合テスト

## テストの実行方法

### Android Studioでの実行

1. Android Studioでプロジェクトを開く
2. `app/src/androidTest/` ディレクトリのテストファイルを右クリック
3. "Run Tests" を選択
4. エミュレーターまたは実機でテストが実行される

### コマンドラインでの実行

```bash
# すべてのAndroidテストを実行
./gradlew connectedDebugAndroidTest

# 特定のテストクラスのみ実行（UIテスト）
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenUITest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.history.HistoryScreenUITest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.details.DetailModalUITest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.auth.AuthScreenUITest

# 特定のテストクラスのみ実行（統合テスト）
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenIntegrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.history.HistoryScreenIntegrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.details.DetailModalIntegrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.auth.AuthScreenIntegrationTest

# 特定のテストメソッドのみ実行
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenUITest#trackScreen_初期状態_基本要素が表示される
```

### 前提条件

- Android エミュレーターが起動しているか、USBデバッグが有効な実機が接続されている
- API レベル 26 以上（minSdk に合わせて）

## テストの特徴

### モック使用によるコンポーネント分離

- `mockk` を使用してViewModelと依存関係をモック化
- UI コンポーネントのみに焦点を当てたテスト
- ビジネスロジックから分離された純粋なUI テスト
- `ErrorMapper`のモック化により、エラーメッセージの一貫性を検証

### 包括的なUI状態カバレッジ

- **UiState<T>パターンの3状態**: Loading, Success, Error
- 初期状態、ローディング状態、エラー状態
- データありなし状態
- インタラクション状態
- 特殊な業務状態（フィナーレ確認など）

### Now in Androidアーキテクチャへの対応

- `UiState<T>`による統一的な状態管理のテスト
- `ErrorMapper`を使用したエラーハンドリングのテスト
- 型安全な状態遷移の検証（exhaustive when式）

### 実用的なインタラクションテスト

- ボタンクリック、テキスト入力
- ナビゲーション、コールバック検証
- 状態変化に伴うUI更新の確認

## テストの意図

これらのテストは以下の目的で実装されています：

1. **リファクタリング時のデグレ検出** - UI構造の変更時に意図しない破損を検出
2. **機能追加時の影響確認** - 新機能追加時に既存UIへの影響を検証
3. **UI品質の担保** - 継続的にUIの動作を保証
4. **ドキュメント効果** - テストが仕様書としての役割を果たす

## 注意事項

- これらはUI レベルのテストのため、実際のデータアクセスやネットワーク処理は含まれません
- ViewModel の詳細なビジネスロジックテストは `app/src/test/` の単体テストで実施してください
- テスト実行には実機またはエミュレーターが必要です