# Testing Samples and Examples

このディレクトリには、プロジェクトで使用されているテストアーキテクチャの実装例とサンプルコードが含まれています。

## 構成

### `viewmodel/` - ViewModelテストの例
- `ViewModelTestabilityDemoTest.kt` - ViewModelテスト容易性向上のデモンストレーション
- `ViewModelImplementationTestingExampleTest.kt` - ViewModel実装テストの包括的な例

### `architecture/` - アーキテクチャレベルのテスト例
- `CleanTestingApproachDemoTest.kt` - プロダクションコードを汚染しないテストアプローチ
- `HiltTestingExamples.kt` - Hilt依存注入フレームワークのテスト例

### `ui/` - UIコンポーネントテストの例
- `AuthScreenTest.kt` - 認証画面のテスト例
- `IntegratedScreenViewModelTest.kt` - 統合画面テストの例
- `TrackScreenTest.kt` - トラック画面の複雑なUI状態テスト例

### `comparisons/` - テストアプローチの比較
- `BeforeAfterComparisonTest.kt` - 従来のテストアプローチと改善後の比較

## ドキュメント

- `E2E_TESTING_GUIDE.md` - エンドツーエンドテストの実装ガイド
- `VIEWMODEL_IMPLEMENTATION_TESTING_GUIDE.md` - ViewModel実装テストの詳細ガイド

## 使用方法

これらのサンプルは以下の目的で使用できます：

1. **学習リソース**: 新しいテストアーキテクチャの理解
2. **テンプレート**: 新しいテストを作成する際のベース
3. **ベストプラクティス**: 推奨されるテスト実装パターンの参考
4. **比較資料**: 従来のアプローチと新しいアプローチの違いを理解

各サンプルファイルには詳細なコメントが含まれており、実装の意図と使用方法が説明されています。