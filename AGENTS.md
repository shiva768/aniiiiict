# Agent Instructions

This document provides instructions for AI agents working on this project.

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
