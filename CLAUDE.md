# CLAUDE.md

Claude Code (claude.ai/code) がこのリポジトリで作業する際のガイド。

- **発言は日本語で行うこと。**
- Aniiiiict は [Annict](https://annict.com)（アニメ視聴管理サービス）の非公式 Android クライアント。Annict OAuth でログインし、視聴中/視聴予定の管理・エピソード記録・履歴閲覧を行う。AniList（GraphQL）と MyAnimeList（REST）を補助メタデータに利用。

## 必ず守ること
- **JDK 17 必須**。Gradle 実行前に `export JAVA_HOME=/Users/shiva768/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`
- コミット前に `./gradlew ktlintFormat && ./gradlew check` を実行
- **UI変更を伴う実装後は必ず `./gradlew connectedDebugAndroidTest`**（デバイス/エミュレーター接続必須）
- ブランチは `master` から切る（`feat/issue-xx` / `fix/issue-xx`）
- UI テキストは日本語

## 詳細ドキュメント（索引）
- [ビルド & 開発コマンド](docs/build.md) — 全コマンド、secrets、JDK設定
- [アーキテクチャ](docs/architecture.md) — レイヤー構成、データフロー、主要パターン、ViewModelテンプレート、API連携、依存関係
- [テスト規約](docs/testing.md) — 3種のテスト、命名、Turbine/MockK
- [コーディング規約](docs/conventions.md) — import、行長、ブランチ命名など
