# コーディング規約

- Kotlin は明示的 import（ワイルドカード・FQCN 禁止）
- `hiltViewModel<T>()` は型パラメータを明示して使う（`androidx.hilt.lifecycle.viewmodel.compose` から import）
- 最大行長: 120文字（detekt）
- UI テキストは日本語
- ブランチ命名: `master` から `feat/issue-xx`, `fix/issue-xx`
- コミット前に `./gradlew ktlintFormat && ./gradlew check` を実行
- ファイル移動・リネームは履歴保持のため `git mv` を使う
