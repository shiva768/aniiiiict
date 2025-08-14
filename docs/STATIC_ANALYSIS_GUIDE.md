# 静的解析ツール設定確認ガイド

このドキュメントは、導入した静的解析ツール（detekt、ktlint）が正しく設定されていることを確認する方法を説明します。

## 設定ファイルの確認

### 1. 必要なファイルが存在することを確認

```bash
# Detekt設定ファイル
ls -la detekt.yml

# EditorConfig設定ファイル（ktlint用）
ls -la .editorconfig

# バージョンカタログにdetektとktlintが定義されているか確認
grep -E "detekt|ktlint" gradle/libs.versions.toml
```

### 2. Gradleプラグインが正しく適用されているか確認

```bash
# ルートプロジェクトの設定確認
grep -A5 -B5 "detekt\|ktlint" build.gradle.kts

# appモジュールの設定確認
grep -A5 -B5 "detekt\|ktlint" app/build.gradle.kts
```

## 静的解析の実行

### 基本的な実行方法

```bash
# 全ての静的解析を実行
./gradlew staticAnalysis

# detektのみ実行
./gradlew detekt

# ktlintチェックのみ実行
./gradlew ktlintCheck

# ktlintフォーマットのみ実行
./gradlew ktlintFormat
```

### 便利なカスタムタスク

```bash
# コードフォーマットを実行
./gradlew formatCode

# コードスタイルをチェック
./gradlew checkCodeStyle
```

## 期待される動作

### Detekt
- コードの複雑度が高い箇所の検出
- 潜在的なバグの可能性がある箇所の指摘
- マジックナンバーの検出
- 未使用のimportの検出
- 空のcatchブロックの検出

### ktlint
- インデントの統一（スペース4つ）
- 最大行長の制限（120文字）
- トレイリングスペースの除去
- ファイル末尾の改行の強制
- import文の整理

## トラブルシューティング

### ビルドエラーが発生する場合

1. Gradle Daemonをクリーンアップ
```bash
./gradlew --stop
./gradlew clean
```

2. 依存関係を再取得
```bash
./gradlew --refresh-dependencies
```

### 特定のルールを無効化したい場合

`detekt.yml`ファイルで該当するルールの`active`を`false`に設定してください。

### IDEとの統合

Android StudioやIntelliJ IDEAに以下のプラグインをインストールすることを推奨します：
- detekt プラグイン
- ktlint プラグイン

## CI/CDでの確認

GitHub Actionsワークフローで静的解析が実行されることを確認：

```bash
# ワークフローファイルの確認
grep -A3 -B3 "staticAnalysis" .github/workflows/build-apk.yml
```

プルリクエスト作成時に静的解析が自動実行され、問題がある場合はビルドが失敗します。