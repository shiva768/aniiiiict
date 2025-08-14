#!/bin/bash

# Build Environment Validator
# This script checks if the environment is ready for Android development

set -e

echo "🔧 Android開発環境をチェックしています..."
echo ""

# Check Java version
echo "📋 Java バージョン確認:"
if java -version 2>&1 | grep -q "version.*17\|version.*1[8-9]\|version.*[2-9][0-9]"; then
    echo "✅ Java 17+ が検出されました"
    java -version 2>&1 | head -1
else
    echo "❌ Java 17以上が必要です"
    echo "現在のJavaバージョン:"
    java -version 2>&1 | head -1 || echo "Javaが見つかりません"
    exit 1
fi

echo ""

# Check Gradle version  
echo "📋 Gradle バージョン確認:"
if ./gradlew --version > /dev/null 2>&1; then
    echo "✅ Gradle Wrapper が利用可能です"
    ./gradlew --version | grep "Gradle"
else
    echo "❌ Gradle Wrapper に問題があります"
    exit 1
fi

echo ""

# Check network connectivity
echo "📋 ネットワーク接続確認:"
./scripts/network-check.sh | grep -E "✅|❌"

echo ""

# Check if build is possible
echo "📋 ビルド可能性テスト:"
if curl -s --head --max-time 5 "https://dl.google.com" > /dev/null 2>&1; then
    echo "✅ Google Maven Repository にアクセス可能"
    echo "🎉 ビルドが可能です！"
    echo ""
    echo "次のコマンドでビルドを開始できます:"
    echo "  ./gradlew build"
else
    echo "❌ Google Maven Repository にアクセスできません"
    echo "⚠️  現在の環境ではビルドできません"
    echo ""
    echo "解決方法については以下を参照してください:"
    echo "  docs/NETWORK_REQUIREMENTS.md"
    echo "  BUILD_ERROR_ANALYSIS.md"
fi

echo ""
echo "🔗 詳細情報:"
echo "  - ネットワーク要件: docs/NETWORK_REQUIREMENTS.md"
echo "  - ビルドエラー解析: BUILD_ERROR_ANALYSIS.md"
echo "  - 開発環境設定: README.md"