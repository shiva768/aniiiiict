# Agent Instructions

This document provides instructions for AI agents working on this project.

## Project Overview

**Aniiiiict** is an Android application for tracking anime broadcasts and viewing history. It integrates with:
- **Annict**: GraphQL API for anime data, broadcast schedules, and viewing records
- **MyAnimeList**: REST API for additional anime metadata (episode counts, images, etc.)

The app allows users to:
- View broadcast schedules by day of the week
- Record watched episodes
- Manage viewing history
- View detailed anime information

## Project Structure

```
app/src/main/java/com/zelretch/aniiiiict/
├── ui/                      # UI Layer (Compose)
│   ├── track/              # 放送スケジュール画面 (Broadcast Schedule)
│   ├── library/            # ライブラリ画面 (Library - WATCHING/WANNA_WATCH)
│   ├── history/            # 記録履歴画面 (Viewing History)
│   ├── animedetail/        # アニメ詳細画面 (Anime Detail)
│   ├── auth/               # 認証画面 (Authentication)
│   ├── settings/           # 設定画面 (Settings)
│   ├── common/             # 共通コンポーネント (Shared components)
│   │   └── components/     # 再利用可能なUIコンポーネント
│   ├── base/               # Base UI components (UiState, ErrorMapper)
│   └── theme/              # Material3 theme
├── domain/                  # Domain Layer
│   ├── usecase/            # Business logic (UseCases)
│   ├── filter/             # Domain filters (ProgramFilter)
│   └── error/              # Domain errors (DomainError)
├── data/                    # Data Layer
│   ├── repository/         # Data repositories
│   ├── api/                # API clients (Annict GraphQL, MAL REST)
│   ├── auth/               # Authentication logic
│   ├── datastore/          # Local storage (DataStore)
│   └── model/              # Data models
├── di/                      # Dependency Injection (Hilt modules)
└── util/                    # Utility classes

app/src/test/               # Unit Tests (JUnit5)
app/src/androidTest/        # Instrumentation Tests (UITest, IntegrationTest)
```

## Tech Stack

### Core Technologies
- **Jetpack Compose**: Declarative UI framework
- **Kotlin**: Primary language (Coroutines, Flow)
- **Material3**: Design system

### Architecture
- **MVVM**: Model-View-ViewModel pattern
- **"Now in Android" Pattern**: UiState<T>, Result<T>, DomainError
- **Clean Architecture**: UI → Domain → Data layers

### Dependency Injection
- **Hilt**: Compile-time DI framework
- `@HiltViewModel`, `@Inject`, `@HiltAndroidApp`

### Networking
- **Apollo Kotlin**: GraphQL client for Annict API
- **Retrofit**: REST client for MyAnimeList API
- **OkHttp**: HTTP client

### Storage
- **DataStore**: Key-value storage (auth tokens, preferences)

### Navigation
- **Navigation Compose**: Type-safe navigation with `NavController`

### Testing
- **JUnit5**: Test framework
- **MockK**: Mocking library
- **Turbine**: Flow testing
- **Compose UI Test**: Instrumentation tests

### Code Quality
- **Ktlint**: Kotlin linter
- **Detekt**: Static code analysis
- **Timber**: Logging

## Navigation Structure

The app uses Navigation Compose with the following screens:

```kotlin
sealed class Screen(val route: String) {
    object Track : Screen("track")              // 放送スケジュール (Main screen)
    object History : Screen("history")          // 記録履歴
    object Settings : Screen("settings")        // 設定
    object AnimeDetail : Screen("anime/{workId}") // アニメ詳細
}
```

- **Bottom Sheet Navigation**: `DetailModal` for episode recording
- **Drawer Navigation**: Side drawer for History and Settings
- **Deep Links**: `anime/{workId}` for anime details

## API Integration

### Annict (GraphQL)
**Primary data source** for:
- Broadcast schedules (`viewer.programs`)
- Anime metadata (title, synopsis, etc.)
- Viewing records (`viewer.records`)
- Streaming platforms (`work.programs.channel`)
- Related works (`work.seriesWorks`)

**Client**: `ApolloClient` via `AnnictApiClient`
**Authentication**: OAuth 2.0 (PKCE) with access token in `Authorization: Bearer`

### MyAnimeList (REST)
**Secondary data source** for:
- Episode counts (`num_episodes`) when Annict data is incomplete
- Fallback images (`main_picture.large`)
- Broadcast status and media type

**Client**: `Retrofit` via `MyAnimeListApi`
**Authentication**: Client ID in `X-MAL-Client-ID` header

### Data Flow
1. **Primary**: Always fetch from Annict first
2. **Fallback**: Use MAL for missing data (episode counts, images)
3. **Local Cache**: DataStore for tokens, preferences (Room planned for episode counts)

## Coding Conventions

### General
- **Language**: Kotlin
- **No FQCNs**: Avoid fully qualified class names in code
- **Imports**: Use explicit imports, not wildcards

### Architecture Patterns
- **Use `UiState<T>`** for screens with data loading (Loading/Success/Error)
- **Inject `ErrorMapper`** into all ViewModels for error handling
- **Use `Result<T>`** for explicit error propagation in repositories
- **Use extension functions** for common ViewModel patterns (`launchWithMinLoadingTime`)

### ViewModels
```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val useCase: YourUseCase,
    private val errorMapper: ErrorMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<YourData>>(UiState.Loading)
    val uiState: StateFlow<UiState<YourData>> = _uiState.asStateFlow()
    
    // Use launchWithMinLoadingTime for consistent loading UX
    fun loadData() {
        launchWithMinLoadingTime {
            useCase().onSuccess { data ->
                _uiState.value = UiState.Success(data)
            }.onFailure { error ->
                _uiState.value = UiState.Error(errorMapper.toUserMessage(error))
            }
        }
    }
}
```

### Compose Screens
- **Use `hiltViewModel<T>()`** with explicit type parameter
- **Import from**: `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`
- **Handle all UiState states**: Loading, Success, Error
- **Separate Composables**: Extract complex UI into separate functions

### Testing
- **Test naming**: Japanese method names with `@DisplayName`
- **Unit Tests**: `app/src/test/` - JUnit5 + MockK
- **Instrumentation Tests**: `app/src/androidTest/` - UITest + IntegrationTest
- **Naming pattern**: `画面名_テスト条件_期待される結果()`

Example:
```kotlin
@Test
@DisplayName("初期状態でローディング表示される")
fun initialStateShowsLoading() {
    // Test implementation
}
```

## Common Development Tasks

### Adding a New Screen

1. **Create UI files** in `ui/yourscreen/`
   - `YourScreen.kt`: Composable UI
   - `YourViewModel.kt`: State management
   - `YourUiState.kt`: UI state data class

2. **Add navigation route** in `MainActivity.kt`
   ```kotlin
   sealed class Screen(val route: String) {
       object YourScreen : Screen("your_screen")
   }
   ```

3. **Add to NavHost**
   ```kotlin
   composable(Screen.YourScreen.route) {
       YourScreen(navController = navController)
   }
   ```

4. **Create tests**
   - `YourScreenUITest.kt`: UI state verification
   - `YourScreenIntegrationTest.kt`: Integration testing

### Adding a New UseCase

1. **Create in `domain/usecase/`**
   ```kotlin
   class YourUseCase @Inject constructor(
       private val repository: YourRepository,
   ) {
       suspend operator fun invoke(): Result<YourData> {
           return repository.getData()
       }
   }
   ```

2. **Inject into ViewModel**
   ```kotlin
   @HiltViewModel
   class YourViewModel @Inject constructor(
       private val yourUseCase: YourUseCase,
       private val errorMapper: ErrorMapper,
   ) : ViewModel()
   ```

3. **Write unit tests** in `app/src/test/`

### Adding API Integration

1. **For GraphQL (Annict)**
   - Add query/mutation in `app/src/main/graphql/`
   - Build generates Kotlin types automatically
   - Use via `AnnictApiClient`

2. **For REST (MAL)**
   - Add endpoint in `MyAnimeListApi.kt`
   - Create response model in `data/model/`
   - Use via `MyAnimeListRepository`

### Running Quality Checks

```bash
# All checks (unit tests + lint + detekt)
./gradlew check

# Unit tests only
./gradlew test

# Instrumentation tests
./gradlew connectedDebugAndroidTest

# Ktlint format
./gradlew ktlintFormat

# Detekt
./gradlew detekt
```

**IMPORTANT:** Always run `./gradlew check` before committing changes. This ensures:
- All unit tests pass
- Code style (ktlint) is correct
- Static analysis (detekt) passes
- No compilation errors

### Component Placement Guidelines

Follow these principles when deciding where to place UI components:

#### Principle: Colocation
- Components should be placed near where they are used
- Screen-specific components → place in screen's directory
- Shared components → place in `ui/common/components/`

#### Principle: Feature-Based Organization
- Group related components by feature or domain
- Use subdirectories to organize complex features
- Avoid generic names like "details" or "utils"

#### When to Move Components
Consider refactoring component placement when:
- A component is used by multiple screens
- Component naming doesn't match its location
- A directory accumulates unrelated components

### Refactoring Workflow

When refactoring code, follow this workflow to maintain quality:

1. **Use `git mv` for file moves/renames**
   - Preserves git history
   - Makes code review easier

2. **Update tests immediately**
   - Tests should be updated in the same PR as the refactoring
   - Ensure all test types are covered (Unit, UI, Integration)

3. **Always run quality checks before committing**
   ```bash
   ./gradlew ktlintFormat  # Auto-fix style issues
   ./gradlew check         # Run all checks (REQUIRED)
   ```

4. **Commit incrementally**
   - Separate logical changes into multiple commits
   - Each commit should build and pass tests
   - Use clear, descriptive commit messages

5. **DO NOT commit:**
   - Environment-specific settings (e.g., `gradle.properties` with local paths)
   - Dump files (*.trc, *.dmp, javacore.*, etc.)
   - Generated files

## Secrets Management

This project requires API client credentials for Annict and MyAnimeList (MAL) to build and run. These are required for all `assemble` and `bundle` tasks, except when only `check` tasks are run.

You need to provide the following secrets:
- `ANNICT_CLIENT_ID`
- `ANNICT_CLIENT_SECRET`
- `MAL_CLIENT_ID`

These can be provided either as environment variables or in a `local.properties` file in the project's root directory.

### Using `local.properties` (Recommended for local development)

1. Create a file named `local.properties` in the root of the project.
2. Add your credentials to the file like this:

```properties
ANNICT_CLIENT_ID="YOUR_ANNICT_CLIENT_ID"
ANNICT_CLIENT_SECRET="YOUR_ANNICT_CLIENT_SECRET"
MAL_CLIENT_ID="YOUR_MAL_CLIENT_ID"
```

The `local.properties` file is included in `.gitignore` and should not be committed to version control.

## Testing

Before submitting any changes, it's important to ensure the quality of the code by running tests.

For any changes, ensure corresponding tests exist for the following types. If not, add them:
- UnitTest
- IntegrationTest
- UITest

### Unit Tests

To run all unit tests and lint checks, use the following command:

```bash
./gradlew check
```

All unit tests must pass before committing. The build will fail if any tests fail.

When submitting, it is required that the unit tests pass without fail.

### Instrumentation Tests (`connectedAndroidTest`)

Instrumentation tests (located in `app/src/androidTest`) require a connected Android device or an emulator. You SHOULD run them locally before submitting changes.

Prerequisites:
- A running Android emulator or a USB-connected device with USB debugging enabled (API level 26+ recommended)
- Required secrets configured (see "Secrets Management"): either environment variables or entries in `local.properties`

Alternatively, run via Gradle directly:

```bash
./gradlew connectedDebugAndroidTest
# or clean and re-run
./gradlew cleanConnectedDebugAndroidTest connectedDebugAndroidTest
```

Reports are generated at:
- `app/build/reports/androidTests/connected/`

CI still runs instrumentation tests automatically on every push. However, please execute them locally (connectedDebugAndroidTest) when developing on your machine to catch issues earlier.

### Test Strategy

#### IntegrationTest

Integration tests verify the collaboration between multiple components (e.g., UseCase + Repository) to ensure they work together as expected.

- Mock external boundaries such as:
  - `Repository`
  - `ProgramFilter` (difficult to inject)
  - `customTabsIntentFactory`
- Do NOT mock domain UseCases that encapsulate logic across repositories (e.g., `JudgeFinaleUseCase`).
  - Instead, provide repository fakes/mocks to drive their behavior.
- Prefer verifying outcomes (data/state changes, UI behavior) rather than internal method-call counts.
- Other mocks should be decided on a case-by-case basis, following the principle of minimizing unnecessary mocking.

#### UITest

- Mock the `ViewModel`.
- Verify the consistency between the `UIState` and the screen's state.
- For screens using `UiState<T>` pattern, verify all three states:
  - `UiState.Loading`: Loading indicator is displayed
  - `UiState.Success<T>`: Data is correctly displayed
  - `UiState.Error`: Error message is displayed

### Test Roles Overview

- **UnitTest**
  - Focus on small, isolated units of code (functions, classes).
  - Use mocks extensively to verify method calls and internal behavior.
  - Appropriate for asserting that specific methods are invoked with expected parameters.

- **IntegrationTest**
  - Focus on collaboration between multiple components (e.g., UseCase + Repository).
  - Prefer verifying outcomes (data/state changes, interactions) rather than internal method calls.
  - Mock external boundaries (Repository, ProgramFilter, customTabsIntentFactory) but avoid mocking domain UseCases.
  - Suitable for checking system-level correctness within module boundaries.

- **UITest**
  - Focus on end-user visible behavior and UI consistency.
  - Do not verify internal method calls.
  - Mock the ViewModel and ensure that the UI state matches the screen’s state and that user interactions produce the correct results.
  - For `UiState<T>` pattern, verify all three states (Loading, Success, Error) are correctly rendered.

## Architecture Patterns

This project follows the **"Now in Android"** architecture pattern, which includes:

### UiState<T> Pattern

All ViewModels use a unified `UiState<T>` sealed interface for state management:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

When implementing ViewModels:
- Use `UiState<T>` for screens with data loading
- Use custom data classes (e.g., `TrackUiState`) for screens with complex state
- Always handle all three states in UI layer

### Error Handling

The project uses explicit error handling with:

1. **DomainError**: Type-safe error hierarchy in domain layer (6 error types)
2. **ErrorMapper**: Converts `DomainError` to user-friendly messages (34 error cases)
3. **Result<T>**: Kotlin's Result type for explicit error propagation

When implementing error handling:
- Inject `ErrorMapper` into ViewModels
- Use `Result.onSuccess` and `Result.onFailure` for explicit error handling
- Map errors to user-friendly messages using `errorMapper.toUserMessage()`

### ViewModel Extensions

Common ViewModel patterns are implemented as extension functions:

- `launchWithMinLoadingTime()`: Ensures minimum 1-second loading display

## Building the Application

To build a debug version of the application (APK), run the following command:

```bash
./gradlew assembleDebug
```

This will generate a debug APK in the `app/build/outputs/apk/debug/` directory.

**Important:** Before building, make sure you have configured the necessary secrets as described in the "Secrets Management" section. The build will fail if the secrets are not provided.
