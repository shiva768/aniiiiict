# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aniiiiict is an unofficial Android client for [Annict](https://annict.com), an anime tracking service. Users log in via Annict OAuth, manage watching/planned anime, record episodes, and view history. It also integrates with AniList (GraphQL) and MyAnimeList (REST) for supplemental metadata.

## Build & Development Commands

```bash
# Build
./gradlew assembleDebug                    # Build debug APK (requires secrets)

# Testing
./gradlew testDebugUnitTest                # Unit tests only
./gradlew connectedDebugAndroidTest        # Instrumentation tests (requires device/emulator)
./gradlew check                            # Unit tests + ktlint + detekt (run before committing)

# Linting & Formatting
./gradlew ktlintCheck                      # Check Kotlin style
./gradlew ktlintFormat                     # Auto-fix style issues
./gradlew detekt                           # Static analysis
```

Secrets (ANNICT_CLIENT_ID, ANNICT_CLIENT_SECRET, MAL_CLIENT_ID) are required for `assembleDebug` but NOT for `check`/test tasks (dummy values used). Configure via `local.properties` (see `local.properties.template`).

## Architecture

**Clean Architecture + MVVM** following the "Now in Android" pattern. Single module (`:app`).

### Layer Structure
- **UI Layer** (`ui/`): Jetpack Compose screens + ViewModels. State managed via `UiState<T>` (Loading/Success/Error) exposed as `StateFlow`.
- **Domain Layer** (`domain/`): UseCases with `Result<T>` return types. `DomainError` sealed class hierarchy for typed errors.
- **Data Layer** (`data/`): Repositories abstract API access. Annict (Apollo GraphQL), AniList (Apollo GraphQL), MyAnimeList (Retrofit REST).

### Data Flow
```
Composable Screen → ViewModel → UseCase → Repository → API Client/DataStore
```

### Key Patterns
- **`UiState<T>`** sealed interface: all screens handle Loading/Success/Error states
- **`DomainError`** sealed class: Network, Auth, Api.ClientError, Api.ServerError, Business, Unknown, Unexpected
- **`ErrorMapper`**: converts `DomainError` to user-facing Japanese messages; inject into all ViewModels
- **`launchWithMinLoadingTime()`**: ViewModel extension ensuring minimum 1s loading display
- **`Result<T>`**: Kotlin Result for explicit error propagation from repositories/use cases

### ViewModel Template
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

## API Integration

- **Annict (primary)**: GraphQL via Apollo. Schemas in `app/src/main/graphql/com.annict/`. OAuth 2.0 Bearer token auth. Callback: `aniiiiict://oauth/callback`
- **AniList (supplemental)**: GraphQL via Apollo. Schemas in `app/src/main/graphql/co.anilist/`. No auth required.
- **MyAnimeList (fallback)**: REST via Retrofit. `X-MAL-Client-ID` header auth. Used for episode counts and images when Annict data is incomplete.

## Testing Conventions

Three test types are required for changes:

- **UnitTest** (`app/src/test/`): JUnit5 + MockK. Test ViewModels and UseCases in isolation.
- **IntegrationTest** (`app/src/androidTest/`): Test UseCase+Repository collaboration. Mock external boundaries (Repository, ProgramFilter) but NOT domain UseCases.
- **UITest** (`app/src/androidTest/`): Mock ViewModel. Verify all three `UiState` states render correctly.

Test naming uses Japanese with `@DisplayName`:
```kotlin
@Test
@DisplayName("初期状態でローディング表示される")
fun initialStateShowsLoading() { ... }
```

JUnit5 with `@Nested` for test organization. Turbine for Flow testing.

## Coding Conventions

- Kotlin with explicit imports (no wildcards, no FQCNs)
- Use `hiltViewModel<T>()` with explicit type parameter (import from `androidx.hilt.lifecycle.viewmodel.compose`)
- Max line length: 120 characters (detekt)
- UI text is in Japanese
- Branch naming: `feat/issue-xx`, `fix/issue-xx` off `master`
- Run `./gradlew ktlintFormat && ./gradlew check` before committing
- Use `git mv` for file moves/renames to preserve history

## Key Dependencies

All versions managed in `gradle/libs.versions.toml`. Key: Compose BOM 2025.06.00, Hilt 2.57.2, Apollo 4.3.3, Retrofit 3.0.0, Kotlin 2.2.20, MinSDK 26, TargetSDK 36, JVM target 17.
