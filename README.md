# Aniiiiict

Annictのアプリケーションです。アニメの視聴記録を管理できます。

## 機能

- Annictアカウントでのログイン
- 視聴中・視聴予定のアニメ一覧の表示
- エピソードごとの視聴記録
- 視聴履歴の管理と表示
- ライブラリ管理（ステータス・クール・メディアでのフィルタリング）
- アニメの詳細情報表示

## 開発環境のセットアップ

### 必要なもの

- Android Studio Meerkat | 2024.3.1 以上
- JDK 17
- Android SDK 36以上

### セットアップ手順

1. リポジトリをクローン

    ```bash
    git clone https://github.com/shiva768/aniiiiict.git
    cd aniiiiict
    ```

2. Annictの開発者アカウントを作成

   - [Annict](https://annict.com)にアクセス
   - [開発者ページ](https://annict.com/oauth/applications)でアプリケーションを作成
   - コールバックURL：`aniiiiict://oauth/callback`
   - スコープ：`read write`

3. 認証情報の設定

   **local.properties（推奨）**

   ```bash
   cp local.properties.template local.properties
   # local.propertiesを編集
   ANNICT_CLIENT_ID=your_client_id_here
   ANNICT_CLIENT_SECRET=your_client_secret_here
   MAL_CLIENT_ID=your_mal_client_id_here
   ```

   - `local.properties` はgitignoreされているため安全
   - テスト・チェック用には dummy 値でOK

   **API認証情報の取得：**

   - Annict API: [開発者ページ](https://annict.com/oauth/applications)で新規アプリケーションを作成
   - MyAnimeList API: [APIコンフィグ](https://myanimelist.net/apiconfig)でClient IDを取得

4. ビルドと実行

   ```bash
   export JAVA_HOME=/path/to/jdk17
   ./gradlew assembleDebug
   ```

## 技術スタック

- [Kotlin](https://kotlinlang.org/) 2.2.20
- [Jetpack Compose](https://developer.android.com/jetpack/compose) BOM 2025.09.00
- [Material3](https://m3.material.io/)
- [Hilt](https://dagger.dev/hilt/) 2.57.2
- [Apollo GraphQL](https://www.apollographql.com/docs/kotlin/) 4.3.3
- [Room](https://developer.android.com/training/data-storage/room) 2.7.0
- [Retrofit](https://square.github.io/retrofit/) 3.0.0
- [OkHttp](https://square.github.io/okhttp/) 5.2.1
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.7
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 1.10.2
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) 2.9.5
- [Coil](https://coil-kt.github.io/coil/) 2.7.0
- [Timber](https://github.com/JakeWharton/timber) 5.0.1

## アーキテクチャ

- **Clean Architecture + MVVM**
- **UiState<T>** による統一的なUI状態管理（Loading/Success/Error）
- **DomainError** による明示的なエラー型定義
- **ErrorMapper** によるユーザー向けエラーメッセージ変換
- Repository パターンによるデータアクセスの抽象化

詳細は[PROJECT_OVERVIEW.md](./docs/PROJECT_OVERVIEW.md)を参照してください。

## テスト

- JUnit 5 + MockK - ユニットテスト・インストルメンテーションテスト
- Turbine - Flowテスト
- Compose UIテスト - コンポーザブルテスト
- Hilt - テスト用DI

```bash
./gradlew testDebugUnitTest          # ユニットテスト
./gradlew connectedDebugAndroidTest  # インストルメンテーションテスト（デバイス/エミュレーター必須）
./gradlew check                      # ユニットテスト + ktlint + detekt
```

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。
