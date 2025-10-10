# Compose UI Tests

このプロジェクトでは、主要な画面に対してCompose UIテストを導入しています。
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

テストファイルは `app/src/androidTest/java/com/zelretch/aniiiiict/ui/` 配下に配置されています。

各画面ごとに以下の2つのファイルが存在します：
- `*UITest.kt`: UI層のみのテスト
- `*IntegrationTest.kt`: 統合テスト

## テストの実行方法

### Android Studioでの実行

1. Android Studioでプロジェクトを開く
2. `app/src/androidTest/` ディレクトリのテストファイルを右クリック
3. "Run Tests" を選択
4. エミュレーターまたは実機でテストが実行される

### コマンドラインでの実行

```bash
# すべてのInstrumentation Testを実行
./gradlew connectedDebugAndroidTest

# 特定のテストクラスのみ実行
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenUITest

# 特定のテストメソッドのみ実行
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenUITest#trackScreen_初期状態_基本要素が表示される
```

### 前提条件

- Android エミュレーターが起動しているか、USBデバッグが有効な実機が接続されている
- API レベル 26 以上（minSdk に合わせて）
- 必要なシークレット設定（`local.properties` または環境変数）

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

## テスト命名規則

Instrumentation Testでは日本語のメソッド名を使用し、テストの意図を明確にしています：

```kotlin
@Test
fun trackScreen_初期状態_基本要素が表示される() {
    // テスト実装
}
```

命名パターン: `画面名_テスト条件_期待される結果()`

## 注意事項

- これらはUI レベルのテストのため、実際のデータアクセスやネットワーク処理は含まれません
- ViewModel の詳細なビジネスロジックテストは `app/src/test/` の単体テストで実施してください
- テスト実行には実機またはエミュレーターが必要です

## 詳細なテスト戦略

より詳細なテスト戦略については、[AGENTS.md](./AGENTS.md#test-strategy) を参照してください。
