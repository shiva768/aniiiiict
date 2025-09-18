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

### Unit Tests

To run all unit tests and lint checks, use the following command:

```bash
./gradlew check
```

This command will execute all unit tests in the project. The build will fail if any tests fail.

### Instrumentation Tests (`connectedAndroidTest`)

Instrumentation tests (located in the `app/src/androidTest` directory) require a connected Android device or emulator. Due to the limitations of the development environment, you cannot run these tests directly.

These tests are automatically executed by the CI pipeline on every push to the repository. If you need to add or modify instrumentation tests, please push your changes to a branch and the CI will run the tests for you.

## Building the Application

To build a debug version of the application (APK), run the following command:

```bash
./gradlew assembleDebug
```

This will generate a debug APK in the `app/build/outputs/apk/debug/` directory.

**Important:** Before building, make sure you have configured the necessary secrets as described in the "Secrets Management" section. The build will fail if the secrets are not provided.

## コミットするときの最低限の条件

- 修正した内容に対応したテストが存在しているかを、下記のテスト毎に確認し、存在していない場合追記する
  - UnitTest
  - IntegrationTest
  - UITest
- UnitTestが通っていること(./gradlew check)
(AndroidTestは実行できないので、コミット後のCIに任せる)
