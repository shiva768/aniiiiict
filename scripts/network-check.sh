#!/bin/bash

# Network connectivity check for Android development
# This script helps diagnose network issues that prevent Android builds

echo "🔍 Checking network connectivity for Android development..."
echo ""

# List of required domains for Android development
REQUIRED_DOMAINS=(
    "dl.google.com"
    "maven.pkg.jetbrains.space"
    "www.jitpack.io"
    "repo.maven.apache.org"
    "plugins.gradle.org"
)

# Check each domain
for domain in "${REQUIRED_DOMAINS[@]}"; do
    echo -n "Checking $domain... "
    if curl -s --head --max-time 10 "https://$domain" > /dev/null 2>&1; then
        echo "✅ OK"
    else
        echo "❌ BLOCKED"
        BLOCKED_DOMAINS+=("$domain")
    fi
done

echo ""

if [ ${#BLOCKED_DOMAINS[@]} -eq 0 ]; then
    echo "🎉 All required domains are accessible!"
    echo "You can now run: ./gradlew build"
else
    echo "⚠️  The following domains are blocked:"
    for blocked in "${BLOCKED_DOMAINS[@]}"; do
        echo "   - https://$blocked"
    done
    echo ""
    echo "📋 To fix this issue:"
    echo "1. Contact your network administrator to allowlist these domains"
    echo "2. Or use a different network connection"
    echo "3. Or configure a proxy that allows access to these domains"
    echo ""
    echo "🔧 Alternative solutions:"
    echo "- Use Android Studio with offline mode (after initial setup)"
    echo "- Use a pre-configured development environment"
fi

echo ""
echo "ℹ️  For more information, see: docs/BUILD_ENVIRONMENT.md"