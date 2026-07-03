# アーキテクチャ

**Clean Architecture + MVVM**（"Now in Android" パターン準拠）。シングルモジュール（`:app`）。

## レイヤー構成
- **UI Layer** (`ui/`): Jetpack Compose 画面 + ViewModel。状態は `UiState<T>`（Loading/Success/Error）を `StateFlow` で公開。
- **Domain Layer** (`domain/`): `Result<T>` を返す UseCase。`DomainError` sealed class 階層で型付きエラー。シングルトンサービス（例: `LibrarySyncService`）が `StateFlow<SyncStatus>` でバックグラウンド同期状態を管理。
- **Data Layer** (`data/`): Repository が API アクセスを抽象化。Annict（Apollo GraphQL）、AniList（Apollo GraphQL）、MyAnimeList（Retrofit REST）。Room（`data/local/`）がライブラリエントリのローカルキャッシュを提供。`AppDatabase` のバージョンは `fallbackToDestructiveMigration` で管理。

## データフロー
```
Composable Screen → ViewModel → UseCase → Repository → API Client/DataStore/Room
```

## 主要パターン
- **`UiState<T>`** sealed interface: 全画面が Loading/Success/Error を処理
- **`DomainError`** sealed class: Network, Auth, Api.ClientError, Api.ServerError, Business, Unknown, Unexpected
- **`ErrorMapper`**: `DomainError` をユーザー向け日本語メッセージに変換。全 ViewModel に注入する
- **`launchWithMinLoadingTime()`**: 最低1秒のローディング表示を保証する ViewModel 拡張
- **`Result<T>`**: Repository/UseCase からの明示的なエラー伝播に Kotlin Result を使用

## ViewModel テンプレート
```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val useCase: XxxUseCase,
    private val errorMapper: ErrorMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<XxxData>>(UiState.Loading)
    val uiState: StateFlow<UiState<XxxData>> = _uiState.asStateFlow()

    fun load() = launchWithMinLoadingTime {
        useCase().onSuccess { _uiState.value = UiState.Success(it) }
            .onFailure { _uiState.value = UiState.Error(errorMapper.toUserMessage(it, "XxxViewModel.load")) }
    }
}
```

## API 連携
- **Annict（主）**: Apollo 経由の GraphQL。スキーマは `app/src/main/graphql/com.annict/`。OAuth 2.0 Bearer トークン認証。コールバック: `aniiiiict://oauth/callback`
- **AniList（補助）**: Apollo 経由の GraphQL。スキーマは `app/src/main/graphql/co.anilist/`。認証不要。
- **MyAnimeList（フォールバック）**: Retrofit 経由の REST。`X-MAL-Client-ID` ヘッダー認証。Annict データが不完全なときのエピソード数・画像に使用。

## 主要依存関係
バージョンは全て `gradle/libs.versions.toml` で管理。主なもの: Compose BOM 2025.09.00, Hilt 2.57.2, Apollo 4.3.3, Retrofit 3.0.0, Kotlin 2.2.20, Room 2.7.0, MinSDK 26, TargetSDK 36, JVM target 17。
