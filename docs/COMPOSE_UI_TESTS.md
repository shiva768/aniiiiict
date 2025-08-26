# Compose UI Tests

このプロジェクトでは、主要な画面（TrackScreen、HistoryScreen）に対してCompose UIテストを導入しています。

## テストファイルの場所

```
app/src/androidTest/java/com/zelretch/aniiiiict/ui/
├── track/TrackScreenComposeTest.kt
└── history/HistoryScreenComposeTest.kt
```

## 実装されたテスト

### TrackScreenのテスト (TrackScreenComposeTest.kt)

- `trackScreen_初期状態_基本要素が表示される()` - 基本的なUI要素の表示確認
- `trackScreen_エラー状態_スナックバーとエラーメッセージが表示される()` - エラー状態のUI検証
- `trackScreen_フィルターボタンクリック_ViewModelメソッドが呼ばれる()` - インタラクションテスト
- `trackScreen_番組リスト_プログラムカードが表示される()` - データ表示テスト
- `trackScreen_フィナーレ確認_適切なスナックバーが表示される()` - 特殊状態のUI検証
- `trackScreen_フィナーレ確認_はいボタンクリック()` - 確認ダイアログのインタラクション
- `trackScreen_履歴ナビゲーション_コールバックが呼ばれる()` - ナビゲーションテスト

### HistoryScreenのテスト (HistoryScreenComposeTest.kt)

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

# 特定のテストクラスのみ実行
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=aniiiiictui.track.TrackScreenComposeTest

# 特定のテストメソッドのみ実行
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=aniiiiictui.track.TrackScreenComposeTest#trackScreen_初期状態_基本要素が表示される
```

### 前提条件

- Android エミュレーターが起動しているか、USBデバッグが有効な実機が接続されている
- API レベル 26 以上（minSdk に合わせて）

## テストの特徴

### モック使用によるコンポーネント分離

- `mockk` を使用してViewModelと依存関係をモック化
- UI コンポーネントのみに焦点を当てたテスト
- ビジネスロジックから分離された純粋なUI テスト

### 包括的なUI状態カバレッジ

- 初期状態、ローディング状態、エラー状態
- データありなし状態
- インタラクション状態
- 特殊な業務状態（フィナーレ確認など）

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