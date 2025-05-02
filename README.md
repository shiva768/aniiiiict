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
- Gradle 8.9.1以上

### セットアップ手順

1. リポジトリをクローン

```bash
git clone https://github.com/your-username/aniiiiiict.git
cd aniiiiiict
```

2. Annictの開発者アカウントを作成

- [Annict](https://annict.com)にアクセス
- アカウントを作成（まだの場合）
- [開発者ページ](https://annict.com/oauth/applications)にアクセス
- 「新しいアプリケーション」をクリック
- 以下の情報を入力：
    - アプリケーション名：Aniiiiict
    - コールバックURL：`aniiiiiict://oauth/callback`
    - スコープ：`read write`

3. 認証情報の設定

- `local.properties.example`を`local.properties`にコピー

```bash
cp local.properties.example local.properties
```

- `local.properties`を編集し、以下の項目を設定：
    - `sdk.dir`：Android SDKのパス
    - `ANNICT_CLIENT_ID`：Annictで取得したClient ID
    - `ANNICT_CLIENT_SECRET`：Annictで取得したClient Secret
  - `ANNICT_ACCESS_TOKEN`：Annictで取得したAccess Token（オプション）

4. ビルドと実行

- Android Studioでプロジェクトを開く
- Gradleの同期を実行
- アプリをビルドして実行

## 技術スタック

- [Kotlin](https://kotlinlang.org/) 2.0.0 - プログラミング言語
- [Jetpack Compose](https://developer.android.com/jetpack/compose) 2025.04.00 - 最新のUIフレームワーク
- [Material3](https://m3.material.io/) 1.3.2 - マテリアルデザインコンポーネント
- [Hilt](https://dagger.dev/hilt/) 2.56 - 依存性注入
- [Apollo GraphQL](https://www.apollographql.com/docs/kotlin/) 3.8.5 - GraphQLクライアント
- [Retrofit](https://square.github.io/retrofit/) 2.11.0 - HTTPクライアント
- [OkHttp](https://square.github.io/okhttp/) 4.12.0 - HTTPクライアント
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.4 - データ永続化
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.8.0 - 非同期処理
- [Coil](https://coil-kt.github.io/coil/) 2.5.0 - 画像読み込み
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) 2.8.9 - 画面遷移

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
- Robolectric 4.10.3 - Androidテスト
- Espresso 3.6.1 - UIテスト
- Compose UIテスト - コンポーザブルテスト

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。