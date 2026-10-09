# ビルド & 開発コマンド

**JDK 17 が必須。** デフォルトJDKが17でないことがあるため明示的に設定する:
```bash
export JAVA_HOME=/Users/shiva768/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
```

```bash
# ビルド
./gradlew assembleDebug                    # デバッグAPKをビルド（secrets必須）
./gradlew assembleRelease                  # リリースAPKをビルド（R8有効・secrets必須）

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

## release ビルド（普段使い）
- 実際に使うのは **R8 を有効にした release APK**。debug ビルドは Compose が最適化されず遅い。
- 個人利用なので、release もデバッグ用 keystore で署名する（`signingConfig = debug`）。
- CI（`.github/workflows/build-apk.yml`）は通常 release APK だけを作る。debug APK は手動実行（workflow_dispatch）のときだけ作る。

### R8 の keep ルール（`app/proguard-rules.pro`）
- **Gson でリフレクション変換するモデルを追加・変更したら、`-keep` ルールを足す**。`@SerializedName` が無いフィールドを R8 がリネームすると、エラーも出さずに null になる。
  - 現在の対象: `TokenResponse`（OAuth）、`MyAnimeList*`（MAL API）
- Apollo / Hilt / Room はコード生成なので通常は不要。リフレクションを使う依存を足したときは、`assembleRelease` した APK で動作確認する。

ベースパッケージ: `com.zelretch.aniiiiict`
