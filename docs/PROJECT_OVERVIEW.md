# Aniiiiict: 技術仕様・アーキテクチャガイド

このドキュメントは、Aniiiiictプロジェクトの現在の仕様、アーキテクチャ、および開発ワークフローの全貌を把握するための一元的な資料です。

## 1. 概要 (Overview)

**Aniiiiict**は、アニメの視聴記録・管理サービス [Annict](https://annict.com) のための非公式Androidクライアントアプリケーションです。ユーザーはAnnictアカウントでログインし、視聴中のアニメの管理、エピソードごとの視聴記録、視聴履歴の確認などを行うことができます。

### 主な機能

- AnnictのOAuth 2.0を利用した安全なログイン
- 視聴中、視聴予定などのステータスごとのアニメ一覧表示
- 作品のシーズン、メディア種別、放送チャンネルなどに基づく高度な絞り込み機能
- エピソードを視聴済みとして記録する機能
- 最終話を視聴した際に、作品全体のステータスを「視聴済み」に更新するかの確認機能
- 過去の視聴記録を一覧で確認できる履歴画面
- 記録の削除機能

## 2. 技術スタックと主要ライブラリ (Tech Stack and Key Libraries)

本プロジェクトは、モダンなAndroid開発で推奨される技術スタックで構築されています。

- **言語:** [Kotlin](https://kotlinlang.org/) 2.1.21
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) 2025.06.00 - UIを宣言的に構築
- **デザイン:** [Material 3](https://m3.material.io/) 1.3.2 - 公式のデザインシステム
- **DI（依存性注入）:** [Hilt](https://dagger.dev/hilt/) 2.57 - DIコンテナ
- **非同期処理:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html) 1.9.0
- **ネットワーキング:**
  - [Apollo Kotlin](https://www.apollographql.com/docs/kotlin/) 4.3.2 - Annict/AniList API用のGraphQLクライアント
  - [OkHttp](https://square.github.io/okhttp/) 4.12.0 - HTTPクライアント
- **データ永続化:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) 1.1.6 - フィルタ設定などの小規模データ保存
- **画像読み込み:** [Coil](https://coil-kt.github.io/coil/) 2.5.0
- **ナビゲーション:** [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) 2.9.0
- **ロギング:** [Timber](https://github.com/JakeWharton/timber) 5.0.1
- **テスト:**
  - [JUnit5](https://junit.org/junit5/) 5.11.4 - テストフレームワーク
  - [MockK](https://mockk.io/) 1.13.10 - Kotlin向けモックライブラリ
  - [Turbine](https://github.com/cashapp/turbine) 1.0.0 - Flowテスト用ライブラリ
  - [Compose UI Tests](https://developer.android.com/jetpack/compose/testing)

## 3. アーキテクチャ (Architecture)

プロジェクトの基盤として、**Clean Architecture**と**MVVM (Model-View-ViewModel)**を組み合わせたパターンを採用しています。さらに、Android公式の**「Now in Android」**アーキテクチャパターンを導入し、型安全なエラーハンドリングと統一的なUI状態管理を実現しています。これにより、関心事の分離、テストの容易性、保守性の向上を実現しています。

### レイヤー構造

アーキテクチャは以下の3つの主要なレイヤーで構成されています。

1.  **UI (Presentation) Layer:**
    -   **役割:** 画面表示とユーザーインタラクションの処理。
    -   **構成要素:** `Activity`, `Composable Screen`, `ViewModel`。
    -   **詳細:** Jetpack Composeを用いてUIを宣言的に構築します。`ViewModel`がUIの状態(`StateFlow`)を保持し、ユーザーからのイベントを処理します。UIは`ViewModel`から公開される状態を観測し、画面を更新します。
    -   **UI状態管理:** `UiState<T>`という統一的な状態パターンを採用しており、`Loading`、`Success<T>`、`Error`の3状態で画面の状態を型安全に表現します。
    -   **エラーハンドリング:** `ErrorMapper`を使用して、ドメイン層の`DomainError`をユーザー向けの分かりやすいメッセージに変換します。

2.  **Domain Layer:**
    -   **役割:** アプリケーション固有のビジネスロジック（ビジネスルール）のカプセル化。
    -   **構成要素:** `UseCase`, `DomainError`。
    -   **詳細:** このレイヤーは、特定のフレームワーク（Android SDKなど）から完全に独立した、Pure Kotlinモジュールです。`UseCase`は、単一の機能（例: 番組一覧の読み込み、エピソードの記録）を担当し、Dataレイヤーの`Repository`を介してデータを操作します。
    -   **エラー型:** `DomainError`という型安全なエラー階層を定義し、6種類のエラー分類（Network, Auth, Api.ClientError, Api.ServerError, Business, Unknown, Unexpected）でエラーを明示的に表現します。

3.  **Data Layer:**
    -   **役割:** アプリケーションのデータに関するすべての処理。データの取得、保存、管理を担当します。
    -   **構成要素:** `Repository`, `API Client (Apollo)`, `DataStore`。
    -   **詳細:** `Repository`パターンを採用し、`ViewModel`や`UseCase`に対してデータの出所（リモートAPI、ローカルDBなど）を抽象化します。このプロジェクトでは、Annict API (GraphQL) と、データ補完用のAniList API (GraphQL) の2つを主なデータソースとしています。

### データフロー

基本的なデータの流れは一方向であり、以下のようになります。

```mermaid
graph TD
    subgraph UI Layer
        A[Composable Screen] -- User Event --> B(ViewModel)
    end

    subgraph Domain Layer
        C(UseCase)
    end

    subgraph Data Layer
        D(Repository)
        E[API Client / DataStore]
    end

    B -- Executes --> C
    C -- Accesses Data --> D
    D -- Fetches/Saves Data --> E
    E -- Returns Data --> D
    D -- Returns Data --> C
    C -- Returns Data --> B
    B -- Updates State --> A
```

1.  **UI**がユーザーイベントを`ViewModel`に通知します。
2.  `ViewModel`は対応する`UseCase`を実行します。
3.  `UseCase`はビジネスロジックを実行し、`Repository`に必要なデータを要求します。
4.  `Repository`は、APIや`DataStore`からデータを取得し、`UseCase`に返します。
5.  `UseCase`は取得したデータを加工し、`ViewModel`に返します。
6.  `ViewModel`は受け取ったデータでUIの状態(`StateFlow`)を更新し、UIがリアクティブに再描画されます。

### Now in Androidパターンの実装

本プロジェクトでは、Android公式の「Now in Android」アーキテクチャパターンを採用し、以下の要素を実装しています。

#### 1. UiState<T>パターン

すべての画面状態を統一的に管理するため、`UiState<T>`という型安全なsealed interfaceを導入しています。

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

これにより、画面の状態を明示的に表現でき、exhaustive when式で安全に処理できます。

#### 2. DomainError階層

ドメイン層でのエラーを型安全に表現するため、`DomainError`というsealed classを定義しています。

```kotlin
sealed class DomainError : Exception() {
    data class Network(val cause: Throwable) : DomainError()
    data class Auth(val message: String) : DomainError()
    sealed class Api : DomainError() {
        data class ClientError(val statusCode: Int) : Api()
        data class ServerError(val statusCode: Int) : Api()
    }
    data class Business(val message: String) : DomainError()
    data class Unknown(val cause: Throwable) : DomainError()
    data object Unexpected : DomainError()
}
```

#### 3. ErrorMapper

`ErrorMapper`は、ドメイン層の`DomainError`をユーザー向けのメッセージに変換する責務を持ちます。34種類の異なるエラーケースに対応し、ユーザーに分かりやすいエラーメッセージを提供します。

```kotlin
@Singleton
class ErrorMapper @Inject constructor() {
    fun toUserMessage(error: Throwable, context: String): String {
        // DomainErrorやその他の例外を、ユーザー向けのメッセージに変換
        ...
    }
}
```

#### 4. ViewModelExtensions

共通のViewModel処理（最小ローディング時間の確保など）を拡張関数として実装し、コードの重複を削減しています。

```kotlin
fun ViewModel.launchWithMinLoadingTime(
    minLoadingTimeMillis: Long = 1000,
    block: suspend CoroutineScope.() -> Unit
) {
    // 最低1秒のローディング表示を保証
    ...
}
```

#### 5. 明示的なエラーハンドリング

すべてのViewModelで、`Result<T>`型を使用した明示的なエラーハンドリングを実装しています。

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val useCase: ExampleUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel() {
    fun load() = launchWithMinLoadingTime {
        useCase().onSuccess { data ->
            _uiState.value = UiState.Success(data)
        }.onFailure { e ->
            val msg = errorMapper.toUserMessage(e, "ExampleViewModel.load")
            _uiState.value = UiState.Error(msg)
        }
    }
}
```

このアプローチにより、エラーハンドリングが明示的になり、各ViewModelで一貫した方法でエラーを処理できます。

## 4. 主要機能の実装詳細 (Implementation Details of Key Features)

### 認証 (Authentication)

-   **フロー:** AnnictのOAuth 2.0認証フローに基づいています。
-   **実装:**
    1.  `AuthScreen`でユーザーがログインボタンをクリックすると、`MainViewModel`が`AnnictAuthUseCase`を介して認証URLを取得します。
    2.  `CustomTabsIntent`を使用して、Annictの認証ページをアプリ内で開きます。
    3.  ユーザーが認証を許可すると、`aniiiiict://oauth/callback`のコールバックURLにリダイレクトされ、`MainActivity`の`onNewIntent`が受け取ります。
    4.  `MainActivity`は認証コードを`MainViewModel`に渡し、`ViewModel`は`AnnictRepository`の`handleAuthCallback`を呼び出してアクセストークンを取得・保存します。
    5.  トークンは`TokenManager`によって`DataStore`に安全に永続化されます。

### 番組一覧表示 (Program Tracking)

-   **フロー:** 視聴中のアニメ一覧を`TrackScreen`に表示する機能です。
-   **実装:**
    1.  `TrackScreen`が表示されると、`TrackViewModel`が`loadingPrograms`関数を実行します。
    2.  `loadingPrograms`は`LoadProgramsUseCase`を実行します。
    3.  `LoadProgramsUseCase`は`AnnictRepository`の`getRawProgramsData`を呼び出します。
    4.  `AnnictRepositoryImpl`は、Apolloクライアントを使って`ViewerPrograms.graphql`クエリを実行し、APIからデータを取得します。
    5.  取得した生データは`LoadProgramsUseCase`で加工されます。具体的には、作品ごとにエピソードがグループ化され、放送日時でソートされた`ProgramWithWork`のリストに変換されます。
    6.  このリストが`TrackViewModel`に返され、UI状態が更新されて画面に表示されます。

## 5. データモデル (Data Models)

`data/model`パッケージには、APIレスポンスをアプリケーション内で扱いやすいように変換したカスタムデータクラスが定義されています。

-   **`Work`**: アニメ作品の情報を保持します（タイトル、シーズン、メディア種別など）。
-   **`Program`**: 個々の放送予定（エピソード）の情報を保持します（放送日時、チャンネル、エピソード詳細など）。
-   **`Episode`**: エピソード自体の情報を保持します（ID、話数、タイトルなど）。
-   **`ProgramWithWork`**: `Work`と、それに関連する`Program`のリストを組み合わせたモデル。UIレイヤーで扱いやすいように設計されています。
-   **`Record`**: 視聴記録の情報を保持します。

## 6. テスト戦略 (Testing Strategy)

本プロジェクトは、**3種類のテスト**で品質を担保しています。

### テストの種類

-   **UnitTest** (`app/src/test`): UseCase、ViewModel等の単体テスト。MockKを使用。
-   **IntegrationTest** (`app/src/androidTest`): UI操作からViewModel、UseCaseを経由してRepository（モック）までの統合テスト。
-   **UITest** (`app/src/androidTest`): ViewModelをモックし、UI層のみをテスト。

### 基本原則

-   **プロダクションコードの純粋性:** `ViewModel`などのプロダクションコードには、テスト専用のコードを一切含めません。
-   **明示的なエラーハンドリング:** `Result<T>`と`ErrorMapper`を使用した型安全なエラー処理により、テストが容易。
-   **JUnit5 + @Nested + @DisplayName**: テストの構造化と可読性の向上。

### ViewModelテストの例

```kotlin
@HiltViewModel
class TrackViewModel @Inject constructor(
    private val loadProgramsUseCase: LoadProgramsUseCase,
    private val errorMapper: ErrorMapper,
    ...
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()
    
    fun loadPrograms() = launchWithMinLoadingTime {
        loadProgramsUseCase().onSuccess { programs ->
            _uiState.value = TrackUiState(programs = programs)
        }.onFailure { e ->
            _uiState.value = TrackUiState(
                error = errorMapper.toUserMessage(e, "TrackViewModel.loadPrograms")
            )
        }
    }
}
```

```kotlin
// テストコード（app/src/test）
@Test
@DisplayName("プログラム読み込みが成功した場合、UI状態が更新される")
fun withSuccessfulLoad() = runTest {
    // Given
    val mockPrograms = listOf(...)
    coEvery { loadProgramsUseCase() } returns Result.success(mockPrograms)
    
    // When
    viewModel.loadPrograms()
    advanceUntilIdle()
    
    // Then
    assertEquals(mockPrograms, viewModel.uiState.value.programs)
}
```

### 詳細ガイド
-   [**Compose UIテストガイド**](./COMPOSE_UI_TESTS.md): Jetpack Composeで作成されたUIのテストケース一覧と、その実行方法に関する実践的なガイドです。
-   [**テスト戦略の詳細**](./AGENTS.md#test-strategy): IntegrationTest、UITestの方針とベストプラクティス。

## 7. 開発ワークフロー (Development Workflow)

-   **Issue駆動開発:** すべてのタスクはGitHubのIssueとして管理されます。
-   **ブランチ戦略:**
    -   `master`がメインブランチです。
    -   機能追加やバグ修正は、`feat/issue-xx`や`fix/issue-xx`のようなプレフィックスを持つフィーチャーブランチを作成して行います。
    -   変更はすべてプルリクエストを通じてレビューされ、承認後に`master`にマージされます。
-   **静的解析:** Detektを利用してコード品質を維持しています。

---
