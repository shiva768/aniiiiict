# Aniiiiict

Annictのアプリケーションです。アニメの視聴記録を管理できます。

## 機能

- Annictアカウントでのログイン
- 視聴中のアニメ一覧の表示
- 視聴予定のアニメ一覧の表示
- アニメごとの視聴開始日の設定

## 開発環境のセットアップ

### 必要なもの

- Android Studio
- JDK 17以上
- Android SDK

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

4. ビルドと実行
- Android Studioでプロジェクトを開く
- Gradleの同期を実行
- アプリをビルドして実行

## 技術スタック

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UIフレームワーク
- [Hilt](https://dagger.dev/hilt/) - 依存性注入
- [Retrofit](https://square.github.io/retrofit/) - APIクライアント
- [Room](https://developer.android.com/training/data-storage/room) - ローカルデータベース
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - 非同期処理

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。 