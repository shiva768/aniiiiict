#!/bin/bash

# Compose UI Tests Runner
# このスクリプトはCompose UIテストを実行するためのヘルパーです

set -e

echo "📱 Aniiiiict Compose UI Tests Runner"
echo "=================================="

# エミュレーターまたは実機の確認
echo "🔍 デバイスの確認中..."
if ! adb devices | grep -q "device$"; then
    echo "❌ エラー: 接続されたデバイスまたはエミュレーターが見つかりません"
    echo "   次のいずれかを確認してください："
    echo "   - Android エミュレーターが起動していること"
    echo "   - USB デバッグが有効な実機が接続されていること"
    exit 1
fi

echo "✅ デバイスが見つかりました"
adb devices

# テスト実行オプション
case "${1:-all}" in
    "all")
        echo "🧪 すべてのCompose UIテストを実行します..."
        ./gradlew connectedDebugAndroidTest
        ;;
    "track")
        echo "🧪 TrackScreen のテストを実行します..."
        ./gradlew connectedDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.track.TrackScreenComposeTest
        ;;
    "history")
        echo "🧪 HistoryScreen のテストを実行します..."
        ./gradlew connectedDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.class=com.zelretch.aniiiiict.ui.history.HistoryScreenComposeTest
        ;;
    "clean")
        echo "🧹 テストキャッシュをクリアして全テストを実行します..."
        ./gradlew cleanConnectedDebugAndroidTest connectedDebugAndroidTest
        ;;
    "help"|"-h"|"--help")
        echo "使用方法: $0 [オプション]"
        echo ""
        echo "オプション:"
        echo "  all     すべてのCompose UIテストを実行（デフォルト）"
        echo "  track   TrackScreen のテストのみ実行"
        echo "  history HistoryScreen のテストのみ実行"
        echo "  clean   キャッシュクリア後にすべてのテストを実行"
        echo "  help    このヘルプを表示"
        echo ""
        echo "前提条件:"
        echo "  - Android エミュレーターが起動している、または"
        echo "  - USB デバッグが有効な Android 実機が接続されている"
        echo "  - API レベル 26 以上のデバイス"
        exit 0
        ;;
    *)
        echo "❌ 不明なオプション: $1"
        echo "ヘルプを表示するには: $0 help"
        exit 1
        ;;
esac

echo ""
echo "✅ テスト実行完了"
echo "📊 詳細な結果は app/build/reports/androidTests/connected/ で確認できます"