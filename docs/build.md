# ビルド & 開発コマンド

**JDK 17 が必須。** デフォルトJDKが17でないことがあるため明示的に設定する:
```bash
export JAVA_HOME=/Users/shiva768/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
```

```bash
# ビルド
./gradlew assembleDebug                    # デバッグAPKをビルド（secrets必須）

# テスト
./gradlew testDebugUnitTest                # ユニットテストのみ
./gradlew testDebugUnitTest --tests "com.zelretch.aniiiiict.ui.library.LibraryViewModelTest"  # 単一テストクラス
./gradlew connectedDebugAndroidTest        # 計装テスト（デバイス/エミュレーター接続必須）
./gradlew check                            # ユニットテスト + ktlint + detekt（コミット前に実行）

# Lint & フォーマット
./gradlew ktlintCheck                      # Kotlinスタイルチェック
./gradlew ktlintFormat                     # スタイル自動修正
./gradlew detekt                           # 静的解析
```

Secrets（`ANNICT_CLIENT_ID`, `ANNICT_CLIENT_SECRET`, `MAL_CLIENT_ID`）は `assembleDebug` には必須だが、`check`/テストタスクには不要（ダミー値が使われる）。`local.properties` で設定する（`local.properties.template` 参照）。

ベースパッケージ: `com.zelretch.aniiiiict`
