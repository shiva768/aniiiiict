#!/bin/sh

# プリコミットフック（オプション）
# このファイルを .git/hooks/pre-commit にコピーし、実行権限を付与してください
# chmod +x .git/hooks/pre-commit

echo "Running static analysis before commit..."

# ktlintチェックを実行
echo "Running ktlint check..."
./gradlew ktlintCheck
if [ $? -ne 0 ]; then
    echo "❌ ktlint check failed. Please fix the issues or run './gradlew ktlintFormat' to auto-format."
    exit 1
fi

# detektを実行
echo "Running detekt..."
./gradlew detekt
if [ $? -ne 0 ]; then
    echo "❌ detekt found issues. Please fix them before committing."
    exit 1
fi

echo "✅ Static analysis passed. Proceeding with commit."