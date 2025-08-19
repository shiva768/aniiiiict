# Aniiiiict Architecture Guide

このドキュメントは、Aniiiiictプロジェクトのアーキテクチャ、機能、開発ルールについて概説し、開発者（AIを含む）がプロジェクトの全体像を迅速に理解することを目的としています。

## 1. 概要 (Overview)

- **プロジェクトの目的**: アニメの視聴記録サービス [Annict](https://annict.com) の非公式Androidクライアントアプリケーションです。
- **主な機能**: Annictアカウントでのログイン、視聴記録の管理、視聴ステータスの更新、作品情報の閲覧など。

## 2. アーキテクチャ (Architecture)

このプロジェクトでは、公式に推奨されている **Clean Architecture** と **MVVM (Model-View-ViewModel)** パターンを組み合わせて採用しています。

### レイヤー構造

- **UI (Presentation) Layer**: Jetpack Composeを用いて宣言的にUIを構築しています。`Activity`, `Composable Screen`, `ViewModel` で構成され、状態管理には `StateFlow` を利用しています。
- **Domain Layer**: `UseCase` としてビジネスロジックをカプセル化しています。このレイヤーは特定のフレームワークに依存しないPure Kotlinモジュールとして設計されています。
- **Data Layer**: `Repository` パターンを通じてデータアクセスを抽象化しています。データソースとして、Annict API (GraphQL) と、作品情報補完のためのAniList API (GraphQL) を利用しています。APIクライアントには `Apollo-Kotlin` を使用。設定などの永続化には `DataStore` を利用しています。

### DI (Dependency Injection)

- **Hilt** を使用して、アプリ全体の依存関係を解決しています。`@HiltViewModel`, `@AndroidEntryPoint`, `@Module`, `@Provides` などのアノテーションを活用しています。

### ログ出力 (Logging)

- **Timber** を直接使用してアプリ全体のログ出力を行っています。以前使用していたLogger interfaceは削除され、より簡潔なアプローチを採用しています。
- Timberは `AniiiiictApplication` で初期化され、デバッグビルド時のみログが出力されます。
- ログフォーマット例: `Timber.i("[ClassName][methodName] メッセージ")` や `Timber.e(exception, "[ClassName] エラーメッセージ")`

## 3. 主要な機能と実装箇所

- **認証 (Authentication)**
  - **関連パス**: `ui/auth`, `data/auth`
  - **概要**: OAuth 2.0フローによるAnnict認証。取得したトークンは `TokenManager` と `DataStore` で安全に管理されます。
- **作品一覧・追跡 (Tracking)**
  - **関連パス**: `ui/track`, `domain/usecase/LoadProgramsUseCase.kt`
  - **概要**: Annict APIから現在視聴中のアニメ一覧などを取得して表示します。
- **視聴記録 (Recording)**
  - **関連パス**: `ui/history`, `domain/usecase/LoadRecordsUseCase.kt`, `CreateRecord.graphql`
  - **概要**: エピソードごとの視聴を記録・表示します。
- **作品詳細 (Details)**
  - **関連パス**: `ui/details`
  - **概要**: AniList APIを利用して、作品のカバー画像などの詳細情報を補完的に取得します。

## 4. データフロー (Data Flow)

基本的なデータの流れは以下の通りです。

`UI (Composable)` → `ViewModel` → `UseCase` → `Repository` → `API (Apollo)` / `Local (DataStore)`

- UIからのイベントをViewModelが受け取り、UseCaseを実行します。
- UseCaseがビジネスロジックを処理し、Repositoryを介してデータを取得・加工します。
- Repositoryは、必要に応じてリモート(API)またはローカル(DataStore)のデータソースを切り替えます。
- データの状態変更は、`StateFlow` を通じてリアクティブにUIへ通知されます。

## 5. ビルドと依存関係

- **バージョン管理**: `gradle/libs.versions.toml` を使用して、すべての依存関係を集中管理しています（バージョンカタログ）。
- **APIキー管理**: 環境変数（`ANNICT_CLIENT_SECRET`）から取得したAPIキーが `BuildConfig` に設定され、安全に参照されます。CIやローカル開発では環境変数として設定します。

## 6. テストアーキテクチャ (Testing Architecture)

このプロジェクトでは、**プロダクションコードを汚染しない拡張ベースのテストアプローチ**を採用しています。

### テスト設計の原則

1. **プロダクションコードの純度**: ViewModelやその他のクラスには、ビジネスロジックのみを含める
2. **テストコードの分離**: テスト用の機能は `app/src/test` 配下の拡張として実装
3. **安全性の確保**: テスト専用メソッドが本番環境で呼ばれるリスクを完全に排除

### ViewModelテストの実装

#### プロダクションコード（純粋）
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val annictAuthUseCase: AnnictAuthUseCase,
    // その他の依存関係...
) : BaseViewModel(), MainViewModelContract {
    // ✅ ビジネスロジックのみ、テスト専用コードは一切含まれない
    // ✅ Timberを直接使用してログ出力（Logger interfaceは不要）
}
```

#### テスト専用拡張（app/src/test配下）
```kotlin
// ViewModelTestExtensions.kt
interface TestableMainViewModel {
    fun setUiStateForTest(state: MainUiState)
    fun setErrorForTest(error: String?)
    fun setLoadingForTest(isLoading: Boolean)
}

class MainViewModelTestWrapper(private val viewModel: MainViewModel) : TestableMainViewModel {
    // リフレクションベースの実装でプライベートフィールドにアクセス
}

// 拡張関数で簡潔なテストコードを実現
fun MainViewModel.asTestable(): TestableMainViewModel {
    return MainViewModelTestWrapper(this)
}
```

#### テストでの使用方法
```kotlin
class MainViewModelTest {
    @Test
    fun `エラー状態のテスト`() {
        val viewModel = MainViewModel(...)
        
        // プロダクション用インターフェース
        val contract: MainViewModelContract = viewModel
        
        // テスト専用機能（拡張として分離）
        val testable = viewModel.asTestable()
        
        // 状態を直接設定（テスト用）
        testable.setErrorForTest("テストエラー")
        
        // 結果を検証
        assertEquals("テストエラー", contract.uiState.value.error)
    }
}
```

### テストの種類と使い分け

1. **インターフェースベースのテスト**: UI画面やコンポーネントのテスト（高速・簡単）
2. **実装テスト**: ビジネスロジックの詳細なテスト（実際のロジック実行）
3. **統合テスト**: エンドツーエンドのフロー全体をテスト

### メリット

- **プロダクションビルドの最適化**: テストコードが本番に含まれない
- **安全性**: テストメソッドの誤用リスクがゼロ
- **保守性**: テスト機能とビジネスロジックが明確に分離
- **拡張性**: 他のViewModelでも同じパターンを適用可能

詳細な実装例とガイドは以下のドキュメントを参照してください：
- [CLEAN_TESTING_APPROACH.md](CLEAN_TESTING_APPROACH.md)
- [VIEWMODEL_TESTING_GUIDE.md](VIEWMODEL_TESTING_GUIDE.md)

## 7. 開発ルール

- **ブランチ戦略**
  - メインブランチは `master` です。
  - 機能開発やバグ修正は、`feat/issue-xx` や `fix/issue-xx` のようなプレフィックスを持つブランチを作成して行います。
  - すべての変更は、GitHub上でプルリクエストを作成し、レビューを経て `master` ブランチにマージされます。
- **Issue駆動開発**: 原則として、すべての開発作業はGitHub Issueに基づいて行います。
