# テスト規約

変更には3種類のテストが必要:

- **UnitTest** (`app/src/test/`): JUnit5 + MockK。ViewModel と UseCase を単体でテスト。
- **IntegrationTest** (`app/src/androidTest/`): UseCase + Repository の連携をテスト。外部境界（Repository, ProgramFilter）はモックするが、ドメイン UseCase はモックしない。`app/src/androidTest/testing/` の `FakeAnnictRepository` を使う。
- **UITest** (`app/src/androidTest/`): ViewModel を MockK でモック。UI 状態とインタラクションを検証。ナビゲーションテストは `@HiltAndroidTest` + `FakeAnnictRepository`、コンポーネントテストは Hilt なしで `createAndroidComposeRule<ComponentActivity>()` を使う。

**UI変更を伴う実装後は必ず `./gradlew connectedDebugAndroidTest` を実行する**（デバイス/エミュレーター接続必須）。

テスト名は `@DisplayName` で日本語:
```kotlin
@Test
@DisplayName("初期状態でローディング表示される")
fun initialStateShowsLoading() { ... }
```

JUnit5 の `@Nested` でテストを整理。Flow のテストには Turbine を使用。
