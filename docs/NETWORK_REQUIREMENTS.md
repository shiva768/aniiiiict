# 🌐 ネットワーク要件とトラブルシューティング

## 必須ドメイン

Android開発では以下のドメインへのHTTPSアクセスが**必須**です：

### 🔴 重要 (Android開発に必須)
- `dl.google.com` - Google Maven Repository
  - Android Gradle Plugin
  - AndroidX ライブラリ
  - Google Play Services

### 🟡 推奨 (追加機能用)  
- `maven.pkg.jetbrains.space` - JetBrains Space
  - Compose compiler
- `www.jitpack.io` - JitPack
  - GitHub上のライブラリ
- `repo.maven.apache.org` - Maven Central
  - 一般的なJavaライブラリ
- `plugins.gradle.org` - Gradle Plugin Portal
  - Gradleプラグイン

## 🔍 診断方法

```bash
# ネットワーク接続をチェック
./scripts/network-check.sh

# 詳細な診断
curl -I https://dl.google.com
curl -I https://repo.maven.apache.org/maven2/
```

## 🛠️ 解決方法

### 1. ネットワーク管理者向け

以下のドメインをallowlistに追加してください：

```
# 必須ドメイン
https://dl.google.com/*
https://services.gradle.org/*

# 推奨ドメイン  
https://repo.maven.apache.org/*
https://plugins.gradle.org/*
https://maven.pkg.jetbrains.space/*
https://www.jitpack.io/*
```

### 2. プロキシ環境の場合

```bash
# Gradle用プロキシ設定
export GRADLE_OPTS="-Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080"
export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080"

# 認証が必要な場合
export GRADLE_OPTS="$GRADLE_OPTS -Dhttp.proxyUser=username -Dhttp.proxyPassword=password"
export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyUser=username -Dhttps.proxyPassword=password"
```

### 3. 代替環境

- **Android Studio**: 初回セットアップ後にオフライン開発可能
- **Docker**: 事前設定済みの開発環境
- **GitHub Codespaces**: クラウドベースの開発環境

## 🚨 よくあるエラー

### Plugin was not found
```
Plugin [id: 'com.android.application'] was not found
```
**原因**: `dl.google.com` への接続がブロックされている  
**解決**: Google Maven Repository へのアクセスを許可

### Dependency resolution failed
```
Could not resolve com.android.library:gradle:X.X.X
```
**原因**: AndroidX ライブラリのダウンロードが失敗  
**解決**: 上記のドメインallowlist設定

## 📞 サポート

ネットワーク設定について質問がある場合：

1. IT部門に上記のドメインリストを提供
2. 一時的に別のネットワーク環境で初回ビルドを実行
3. Android Studio での開発を検討