# ビルド環境設定ガイド

## ⚠️ 重要: ネットワーク接続の要件

このプロジェクトのビルドには、以下のドメインへのアクセスが必要です：

- `https://dl.google.com` (Google Maven Repository)
- `https://maven.pkg.jetbrains.space` (JetBrains Space)
- `https://www.jitpack.io` (JitPack)
- `https://repo.maven.apache.org` (Maven Central)
- `https://plugins.gradle.org` (Gradle Plugin Portal)

## 🔍 ネットワーク接続の診断

プロジェクトルートで以下のコマンドを実行してください：

```bash
./scripts/network-check.sh
```

このスクリプトは必要なドメインへの接続を確認し、問題がある場合は解決方法を提示します。

## 🚨 ビルドエラーの解決

### エラー: "Plugin was not found"

```
Plugin [id: 'com.android.application', version: 'X.X.X', apply: false] was not found
```

**原因**: 必要なリポジトリにアクセスできません。

**解決方法**:

1. **ネットワーク管理者への連絡** (推奨)
   - 上記のドメインをallowlistに追加してもらう

2. **プロキシ設定**
   ```bash
   export GRADLE_OPTS="-Dhttp.proxyHost=proxy.company.com -Dhttp.proxyPort=8080"
   export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyHost=proxy.company.com -Dhttps.proxyPort=8080"
   ```

3. **Android Studio使用** (オフライン対応)
   - 初回セットアップ後はキャッシュされた依存関係を使用

## 📋 環境別の対応方法

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