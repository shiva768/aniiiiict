# ビルド環境に関する重要な注意事項

## 概要

このプロジェクトにはAndroid開発と静的解析ツールが含まれており、適切なビルドにはインターネットアクセスが必要です。

## 前提条件

### 必要なネットワークアクセス

ビルドが成功するには、以下のリポジトリへのアクセスが必要です：

1. **Google Maven Repository** (`https://dl.google.com`)
   - Android Gradle Plugin
   - Android SDK関連の依存関係

2. **Gradle Plugin Portal** (`https://plugins.gradle.org`)
   - Detekt、ktlint等の静的解析プラグイン

3. **Maven Central** (`https://repo1.maven.org`)
   - その他のライブラリ依存関係

### ビルド問題のトラブルシューティング

#### 問題: Android Gradle Plugin が見つからない

```
Plugin [id: 'com.android.application', version: 'X.X.X', apply: false] was not found
```

**原因**: Google Maven Repositoryにアクセスできません。

**解決方法**:
1. インターネット接続を確認
2. 企業ネットワークの場合、プロキシ設定を確認
3. Android Studioの環境で実行（キャッシュされた依存関係を使用）

#### 問題: 静的解析プラグインが利用できない

**解決方法**:
環境変数を設定して静的解析をスキップ：

```bash
export SKIP_STATIC_ANALYSIS=true
./gradlew build
```

## 開発環境の設定

### 推奨する開発環境

1. **Android Studio**
   - 事前に依存関係がキャッシュされているため最も確実
   - 自動的にプロキシ設定等を管理

2. **ローカル開発環境**
   - 直接インターネットアクセスがある環境
   - 企業プロキシがある場合は適切に設定

### CI/CD環境での設定

GitHub Actionsやその他のCI環境では、以下の点に注意：

1. **ネットワーク制限**
   - 一部のCI環境では特定のドメインへのアクセスが制限される場合があります
   - 必要なドメインをallowlistに追加

2. **依存関係キャッシュ**
   - Gradleキャッシュを適切に設定してビルド時間を短縮

## 静的解析ツール

### 利用可能なコマンド

ネットワークアクセスが利用可能な環境では：

```bash
# 全ての静的解析を実行
./gradlew staticAnalysis

# コードフォーマット
./gradlew formatCode

# コードスタイルチェック
./gradlew checkCodeStyle

# 個別実行
./gradlew detekt
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### 設定ファイル

- `detekt.yml` - Detekt設定
- `.editorconfig` - エディタ設定（ktlint対応）

## トラブルシューティング

### 開発環境の診断

プロジェクトルートで以下のスクリプトを実行：

```bash
./scripts/setup-dev-tools.sh
```

このスクリプトは：
- ネットワーク接続性をチェック
- 利用可能な機能を表示
- 問題の診断情報を提供

### よくある問題と解決方法

1. **"Version 8.10.1 was not found"**
   - この問題は修正されました（実在しないバージョン番号でした）
   - 現在は安定版AGP 8.1.4を使用

2. **"dl.google.com" へのアクセスができない**
   - 企業ファイアウォールまたはプロキシの問題
   - ネットワーク管理者に相談

3. **静的解析ツールが動作しない**
   - `SKIP_STATIC_ANALYSIS=true` でビルドを継続可能
   - 開発環境では通常通り利用可能

## サポート

ビルドに関する問題が発生した場合：

1. まず `./scripts/setup-dev-tools.sh` を実行
2. エラーメッセージと環境情報を記録
3. ネットワーク制限が原因の場合は、適切な環境での実行を検討