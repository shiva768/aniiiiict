# 静的解析ツール導入に関する技術的な注意事項

## バージョン互換性

現在の設定では以下のバージョンを使用しています：

- **Gradle**: 8.5
- **Android Gradle Plugin**: 8.3.2
- **Detekt**: 1.23.7
- **ktlint Gradle Plugin**: 12.1.1

## 既知の制限事項

### Android Gradle Plugin バージョンの問題

開発環境によっては、指定されたAndroid Gradle Pluginのバージョン（8.3.2）が利用できない場合があります。
この場合は、以下の対応を検討してください：

1. **利用可能なバージョンの確認**
   ```bash
   # Maven Centralで利用可能なAGPバージョンを確認
   curl -s "https://repo1.maven.org/maven2/com/android/tools/build/gradle/maven-metadata.xml" | grep -o '<version>[^<]*</version>'
   ```

2. **より安定したバージョンへの変更**
   `gradle/libs.versions.toml`で以下のように変更：
   ```toml
   agp = "8.1.4"  # より安定したバージョン
   ```

3. **Gradleバージョンとの整合性確認**
   AGPとGradleのバージョン互換性については[公式ドキュメント](https://developer.android.com/studio/releases/gradle-plugin)を参照してください。

## 静的解析ツールの動作確認

ビルドシステムの問題により通常のGradleタスクが実行できない場合でも、設定ファイルの妥当性は以下で確認できます：

### 設定ファイルの構文チェック

```bash
# detekt.yml の構文確認
python -c "import yaml; yaml.safe_load(open('detekt.yml'))" && echo "detekt.yml: OK"

# .editorconfig の基本チェック
grep -q "root = true" .editorconfig && echo ".editorconfig: OK"
```

### 静的解析ツールの直接実行（デバッグ用）

```bash
# detektスタンドアロン実行
curl -sSLO https://github.com/detekt/detekt/releases/download/v1.23.7/detekt-cli-1.23.7.jar
java -jar detekt-cli-1.23.7.jar --config detekt.yml --input app/src

# ktlintスタンドアロン実行
curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.0.1/ktlint
chmod +x ktlint
./ktlint "app/src/**/*.kt"
```

## 推奨される解決手順

1. **環境の確認**: JDK、Android SDK、Gradleバージョンの確認
2. **依存関係の更新**: 使用可能なバージョンへの調整
3. **段階的テスト**: まず設定ファイルのみ、次にプラグインの動作確認
4. **CI環境での検証**: GitHub Actionsでの動作確認

## 長期的な改善案

- Android Gradle Pluginの安定版への移行
- Gradle Version Catalogsの活用拡大
- IDE統合の改善
- プリコミットフックの追加検討