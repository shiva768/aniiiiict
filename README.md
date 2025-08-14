# Aniiiiict

Annictのアプリケーションです。アニメの視聴記録を管理できます。

## 機能

- Annictアカウントでのログイン
- 視聴中のアニメ一覧の表示
- 視聴予定のアニメ一覧の表示
- エピソードごとの視聴記録
- 視聴履歴の管理と表示
- 視聴ステータスの自動更新（視聴予定→視聴中）
- アニメ一覧の絞り込み機能
- アニメの詳細情報表示

## 開発環境のセットアップ

### 必要なもの

- Android Studio Hedgehog | 2023.1.1 以上
- JDK 17以上
- Android SDK 35以上
- Gradle 8.5以上

### セットアップ手順



#### 1. リポジトリをクローン

```bash
git clone https://github.com/your-username/aniiiiiict.git
cd aniiiiiict
```

#### 2. Annictの開発者アカウントを作成

- [Annict](https://annict.com)にアクセス
- アカウントを作成（まだの場合）
- [開発者ページ](https://annict.com/oauth/applications)にアクセス
- 「新しいアプリケーション」をクリック
- 以下の情報を入力：
    - アプリケーション名：Aniiiiict
    - コールバックURL：`aniiiiiict://oauth/callback`
    - スコープ：`read write`

#### 3. 認証情報の設定

- `local.properties.example`を`local.properties`にコピー

```bash
cp local.properties.example local.properties
```

- `local.properties`を編集し、以下の項目を設定：
    - `sdk.dir`：Android SDKのパス
    - `ANNICT_CLIENT_SECRET`：Annictで取得したClient Secret

4. ビルドと実行

- Android Studioでプロジェクトを開く
- Gradleの同期を実行
- アプリをビルドして実行

## 開発ツール

### 静的解析ツール

コードの品質と一貫性を維持するため、以下の静的解析ツールを導入しています：

#### Detekt
- **目的**: コードの複雑度、潜在的なバグ、アンチパターンの検出
- **設定ファイル**: `detekt.yml`
- **実行**: `./gradlew detekt`

#### ktlint
- **目的**: Kotlinコーディングスタイルの強制とフォーマット統一
- **設定ファイル**: `.editorconfig`
- **実行**: 
  - チェック: `./gradlew ktlintCheck`
  - フォーマット: `./gradlew ktlintFormat`

#### 便利なコマンド

```bash
# 全ての静的解析を実行
./gradlew staticAnalysis

# コードフォーマットを実行
./gradlew formatCode

# コードスタイルをチェック
./gradlew checkCodeStyle
```

詳細な設定方法や使用方法については、[静的解析ガイド](docs/STATIC_ANALYSIS_GUIDE.md)を参照してください。

#### オプション：プリコミットフック

コミット前に自動で静的解析を実行したい場合は、以下のコマンドでプリコミットフックを設定できます：

```bash
cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## 技術スタック

- [Kotlin](https://kotlinlang.org/) 2.0.0 - プログラミング言語
- [Jetpack Compose](https://developer.android.com/jetpack/compose) 2025.05.00 - 最新のUIフレームワーク
- [Material3](https://m3.material.io/) 1.3.2 - マテリアルデザインコンポーネント
- [Hilt](https://dagger.dev/hilt/) 2.56 - 依存性注入
- [Apollo GraphQL](https://www.apollographql.com/docs/kotlin/) 4.2.0 - GraphQLクライアント
- [Retrofit](https://square.github.io/retrofit/) 2.11.0 - HTTPクライアント
- [OkHttp](https://square.github.io/okhttp/) 4.12.0 - HTTPクライアント
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.6 - データ永続化
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.9.0 - 非同期処理
- [Coil](https://coil-kt.github.io/coil/) 2.5.0 - 画像読み込み
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) 2.9.0 - 画面遷移
- [Kotlin Compose Plugin](https://developer.android.com/jetpack/compose) 2.0.0 - Compose用Kotlinプラグイン
- [KSP](https://kotlinlang.org/docs/ksp-overview.html) 2.0.0-1.0.21 - Kotlin Symbol Processing
- [Secrets Gradle Plugin](https://github.com/google/secrets-gradle-plugin) 2.0.1 - APIキー管理プラグイン

## ビルド要件

### ネットワークアクセス要件

このプロジェクトを正常にビルドするには、以下のリポジトリへのアクセスが必要です：

- **Google Maven Repository** (`dl.google.com`) - Android Gradle Plugin
- **Gradle Plugin Portal** (`plugins.gradle.org`) - 静的解析プラグイン
- **Maven Central** (`repo1.maven.org`) - その他の依存関係

### 推奨開発環境

1. **Android Studio** - 事前キャッシュされた依存関係で最も確実
2. **直接インターネットアクセスのあるローカル環境**
3. **適切に設定された企業プロキシ環境**

### トラブルシューティング

ビルドに問題がある場合：

```bash
# 環境診断スクリプトを実行
./scripts/setup-dev-tools.sh
```


- [Detekt](https://detekt.dev/) 1.23.7 - 静的解析ツール
- [ktlint](https://ktlint.github.io/) 12.1.1 - Kotlinコードフォーマッター

## アーキテクチャ

- Clean Architecture + MVVMパターンを採用
- ドメインロジックをUseCaseに集約
- Repository パターンによるデータアクセスの抽象化
- Composableによる宣言的UI
- DataStoreによるデータ永続化
- GraphQLによる効率的なデータ取得
- 依存関係のバージョン管理はlibs.versions.tomlで一元化

## テスト

- JUnit 4.13.2 - ユニットテスト
- Mockito 5.10.0 - モックライブラリ
- MockK 1.13.10 - モックライブラリ（Kotlin向け）
- Kotest 5.8.1 - テストフレームワーク
- Robolectric 4.10.3 - Androidテスト
- Espresso 3.6.1 - UIテスト
- Compose UIテスト - コンポーザブルテスト

## 依存関係バージョン管理

- 依存関係のバージョンは`gradle/libs.versions.toml`で一元管理しています。

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。