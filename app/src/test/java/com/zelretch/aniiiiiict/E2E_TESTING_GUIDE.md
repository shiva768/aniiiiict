# E2E-Style Testing Guide

このガイドでは、リポジトリをモック化し、ViewModelからUseCaseまでの実装を使用するE2Eスタイルのテスト方法について説明します。

## E2Eスタイルテストの概要

従来のユニットテストでは、各コンポーネント（ViewModel、UseCase、Repository）を個別にテストし、依存関係をすべてモック化します。一方、E2Eスタイルのテストでは、以下のアプローチを取ります：

1. **リポジトリのみをモック化**: データアクセス層（Repository）のみをモック化し、ビジネスロジック層（UseCase）と表示ロジック層（ViewModel）は実際の実装を使用します。
2. **実際のフロー検証**: ViewModelからUseCaseを経由してRepositoryまでの実際のフローを検証します。
3. **エンドツーエンドの動作確認**: ユーザー操作からUIの状態変化までの一連の流れを確認します。

このアプローチにより、以下のメリットがあります：

- 実際のビジネスロジックが正しく動作することを確認できる
- コンポーネント間の連携が正しく機能することを検証できる
- リポジトリのモックのみで、アプリの主要な機能をテストできる

## 実装例: TrackViewModelE2ETest

`TrackViewModelE2ETest`は、E2Eスタイルのテスト実装例です。以下の特徴があります：

1. **リポジトリのモック化**:

```kotlin
// リポジトリをモック化
val annictRepository = mockk<AnnictRepository>()
val aniListRepository = mockk<AniListRepository>()
```

2. **実際のUseCaseの使用**:

```kotlin
// 実際のUseCaseを使用
val loadProgramsUseCase = LoadProgramsUseCase(annictRepository)
val updateViewStateUseCase = UpdateViewStateUseCase(annictRepository, TestLogger())
val watchEpisodeUseCase = WatchEpisodeUseCase(annictRepository, updateViewStateUseCase)
val programFilter = ProgramFilter()
val filterProgramsUseCase = FilterProgramsUseCase(programFilter)
val judgeFinaleUseCase = JudgeFinaleUseCase(TestLogger(), aniListRepository)
```

3. **ViewModelの初期化**:

```kotlin
viewModel = TrackViewModel(
    loadProgramsUseCase,
    watchEpisodeUseCase,
    filterProgramsUseCase,
    filterPreferences,
    judgeFinaleUseCase,
    TestLogger()
)
```

4. **リポジトリの動作設定**:

```kotlin
// デフォルトのモック動作を設定
coEvery { annictRepository.getRawProgramsData() } returns flowOf(emptyList())
coEvery { annictRepository.createRecord(any(), any()) } returns true
coEvery { annictRepository.updateWorkViewStatus(any(), any()) } returns true
coEvery { aniListRepository.getMedia(any()) } returns Result.success(
    AniListMedia(
        id = 1,
        episodes = 12,
        format = "TV",
        status = "RELEASING",
        nextAiringEpisode = NextAiringEpisode(
            episode = 2,
            airingAt = 0
        )
    )
)
```

5. **テストケースの実装**:

```kotlin
given("プログラム一覧ロード（E2Eスタイル）") {
    `when`("正常にロードできる場合") {
        then("UIStateにプログラムがセットされる") {
            runTest {
                // モックリポジトリの動作を設定
                val mockPrograms = createMockPrograms()
                coEvery { annictRepository.getRawProgramsData() } returns flowOf(mockPrograms)

                // フィルター変更をトリガーにしてロード処理を実行
                filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy"))
                testScope.testScheduler.advanceUntilIdle()

                // UIStateを検証
                viewModel.uiState.test {
                    val initialState = awaitItem() // 初期状態
                    initialState.programs shouldBe emptyList()

                    val loadingState = awaitItem() // ローディング状態
                    loadingState.isLoading shouldBe true

                    val loadedState = awaitItem() // データロード完了状態
                    loadedState.isLoading shouldBe false
                    loadedState.programs.isEmpty() shouldBe false
                    loadedState.error shouldBe null

                    // リポジトリが呼ばれたことを検証
                    coVerify { annictRepository.getRawProgramsData() }
                }
            }
        }
    }
}
```

## 他のViewModelに対するE2Eテストの作成方法

他のViewModel（例：HistoryViewModel、DetailModalViewModel）に対してE2Eスタイルのテストを作成する手順は以下の通りです：

1. **依存関係の分析**:
    - ViewModelが依存するUseCaseを特定する
    - UseCaseが依存するRepositoryを特定する
    - 必要なモックオブジェクトを準備する

2. **テストクラスの作成**:
    - `[ViewModel名]E2ETest.kt`ファイルを作成する
    - BehaviorSpecを継承し、テストケースを記述する

3. **リポジトリのモック化**:
    - 必要なリポジトリをモック化する
    - テストケースに応じたモックの動作を設定する

4. **実際のUseCaseの使用**:
    - ViewModelが依存するUseCaseの実際の実装を使用する
    - UseCaseの依存関係（他のUseCase、ユーティリティクラスなど）も実際の実装を使用する

5. **テストケースの実装**:
    - 主要なユーザーフローに対応するテストケースを実装する
    - UIStateの変化を検証する
    - リポジトリメソッドが正しく呼び出されることを検証する

## 注意点

1. **テスト環境の設定**:
    - コルーチンのテスト環境を適切に設定する（`Dispatchers.setMain(dispatcher)`）
    - テスト用のスコープを使用する（`viewModel.externalScope = testScope`）

2. **非同期処理の扱い**:
    - `runTest`ブロック内でテストを実行する
    - `testScope.testScheduler.advanceUntilIdle()`で非同期処理の完了を待つ

3. **UIStateの検証**:
    - Turbineを使用してFlowの値を検証する（`viewModel.uiState.test { ... }`）
    - 状態遷移を正しく検証する（初期状態→ローディング状態→データロード完了状態）

4. **モックの検証**:
    - `coVerify`を使用してリポジトリメソッドが呼び出されたことを検証する

## まとめ

E2Eスタイルのテストは、リポジトリをモック化しつつ、ViewModelからUseCaseまでの実際の実装を使用することで、アプリケーションの主要なロジックを包括的にテストすることができます。このアプローチにより、ユニットテストでは見つけにくい統合的な問題を発見し、アプリケーションの品質を向上させることができます。