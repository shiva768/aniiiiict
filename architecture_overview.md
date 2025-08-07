## Aniiiiict アプリケーションアーキテクチャ解説

このアプリケーションは、Android開発で推奨されている**クリーンアーキテクチャ**と**MVVM (Model-View-ViewModel)** パターンを組み合わせた、モダンで堅牢な設計を採用しています。

### 全体像

アーキテクチャは、大きく3つの層に分かれています。

1.  **UI層 (Presentation Layer)**: 画面表示とユーザー操作を担当します。
2.  **ドメイン層 (Domain Layer)**: アプリケーションのビジネスロジックを担当します。
3.  **データ層 (Data Layer)**: データへのアクセスと管理を担当します。

これらの層は、**依存関係のルール**に従っています。内側の層（ドメイン層）は、外側の層（UI層、データ層）について何も知りません。これにより、各層が独立して開発・テストできるようになり、変更に強いシステムを実現しています。

```mermaid
graph TD
    subgraph UI層
        A[View (Activity/Composable)] --> B{ViewModel};
    end

    subgraph ドメイン層
        C[UseCase]
    end

    subgraph データ層
        D[Repository] --> E[Remote Data Source (API)];
        D --> F[Local Data Source (DataStore)];
    end

    B --> C;
    C --> D;
```

### 各層の詳細

#### 1. UI層 (Presentation Layer)

*   **役割**: 画面の描画、ユーザーからの入力を受け付け、ViewModelに伝えます。
*   **主要技術**:
    *   **Jetpack Compose**: 宣言的なUIフレームワーク。少ないコードで直感的にUIを構築します。
    *   **ViewModel**: UIの状態(UiState)を保持し、UIに関連するビジネスロジックを扱います。View（Composable）からのイベントを受け取り、UseCaseを呼び出します。
    *   **UiState**: UIの状態を表すデータクラス。`isLoading`, `error` などの状態も含まれ、UIはこれを監視して画面を更新します。
*   **実装例**:
    *   `TrackViewModel.kt`: `TrackScreen` の状態を管理し、`LoadProgramsUseCase` などを呼び出して番組一覧を取得します。
    *   `TrackScreen.kt` (ファイル一覧から推測): `TrackViewModel` が持つ `uiState` を監視し、番組一覧やローディング表示を切り替えます。

#### 2. ドメイン層 (Domain Layer)

*   **役割**: アプリケーション固有のビジネスロジック（どういうデータを、どういう条件で、どう処理するか）を記述します。UI層やデータ層から独立しています。
*   **主要技術**:
    *   **UseCase**: 単一の機能（例：「番組一覧を読み込む」）を担当するクラスです。`operator fun invoke()` を持つことで、インスタンスを関数のように呼び出せます。
*   **実装例**:
    *   `LoadProgramsUseCase.kt`: `AnnictRepository` を通じて番組一覧を取得する、というビジネスロジックのみを担当します。
    *   `FilterProgramsUseCase.kt`: 番組一覧を特定の条件でフィルタリングするロジックを担当します。

#### 3. データ層 (Data Layer)

*   **役割**: アプリケーションが使用するデータの取得、保存、管理を行います。データの取得元（API、データベースなど）を隠蔽し、ドメイン層に一貫したインターフェースを提供します。
*   **主要技術**:
    *   **Repository**: データアクセスの窓口となるインターフェースです。ドメイン層は、このインターフェースのみに依存します。
    *   **RepositoryImpl**: Repositoryインターフェースの実装クラス。ここで、APIからのデータ取得や、ローカルのDataStoreへの保存などの具体的な処理を行います。
    *   **Apollo GraphQL Client**: AnnictのGraphQL APIと通信し、データを取得します。
    *   **Retrofit/OkHttp**: （認証処理などで）REST APIとの通信に使用されます。
    *   **DataStore**: ユーザーのフィルタ設定など、少量のデータを永続化するために使用されます。
*   **実装例**:
    *   `AnnictRepository.kt`: `getProgramsWithWorks()` のような、データ操作のためのメソッドを定義したインターフェース。
    *   `AnnictRepositoryImpl.kt`: `AnnictRepository` を実装し、`ApolloClient` を使って実際にAnnict APIからデータを取得します。

### 依存性の注入 (Dependency Injection)

*   **役割**: クラス間の依存関係を外部から注入することで、クラスの独立性を高め、テストを容易にします。
*   **主要技術**:
    *   **Hilt**: Android向けのDIライブラリ。`@Module`, `@Provides`, `@Inject` などのアノテーションを使うことで、必要なインスタンス（Repository, UseCaseなど）を自動的に生成し、必要な場所に提供します。
*   **実装例**:
    *   `AppModule.kt`: `AnnictRepository` や `OkHttpClient` などのインスタンスをどのように生成するかを定義しています。

### まとめ

このアプリケーションは、クリーンアーキテクチャの原則に則った、関心の分離が明確な設計になっています。

*   **変更に強い**: 例えば、将来APIの仕様が変わっても、影響範囲はデータ層の`AnnictRepositoryImpl` に限定され、UI層やドメイン層を修正する必要はありません。
*   **テストが容易**: 各層が独立しているため、`ViewModel` のテスト、`UseCase` のテスト、`Repository` のテストをそれぞれ個別に、かつ容易に行うことができます。
*   **コードの見通しが良い**: 機能ごとにクラスが分割されているため、どこに何が書かれているかが分かりやすく、メンテナンス性に優れています。

今後の開発依頼の際には、このアーキテクチャを前提として、どの層にどのような変更を加えたいかをご指示いただくと、スムーズに開発を進めることができます。