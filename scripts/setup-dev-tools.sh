#!/bin/bash

# Development Environment Setup and Diagnostic Script
# This script checks the environment and sets up static analysis tools

set -e

echo "🔧 Aniiiiiict Development Environment Diagnostic"
echo "==============================================="
echo ""

# Check if we have network access to required repositories
echo "📡 Checking network connectivity to required repositories..."

check_repo_access() {
    local url=$1
    local name=$2
    if timeout 10 curl -s --head "$url" > /dev/null 2>&1; then
        echo "✅ $name accessible"
        return 0
    else
        echo "❌ $name not accessible"
        return 1
    fi
}

# Check repository accessibility
REPOS_ACCESSIBLE=true

if ! check_repo_access "https://plugins.gradle.org" "Gradle Plugin Portal"; then
    REPOS_ACCESSIBLE=false
fi

if ! check_repo_access "https://dl.google.com" "Google Maven Repository"; then
    REPOS_ACCESSIBLE=false
fi

if ! check_repo_access "https://repo1.maven.org" "Maven Central"; then
    REPOS_ACCESSIBLE=false
fi

echo ""

if [ "$REPOS_ACCESSIBLE" = "false" ]; then
    echo "⚠️  NETWORK ACCESS LIMITED"
    echo "   Some required repositories are not accessible."
    echo "   This is common in:"
    echo "   - CI/CD environments with restricted networking"
    echo "   - Corporate networks with firewalls"
    echo "   - Offline development environments"
    echo ""
    echo "💡 RECOMMENDATIONS:"
    echo "   1. For building: Use Android Studio (has cached dependencies)"
    echo "   2. For CI/CD: Configure allowlist for required domains"
    echo "   3. For corporate networks: Configure proxy settings"
    echo ""
    echo "   Static analysis will be disabled for this environment."
    export SKIP_STATIC_ANALYSIS=true
else
    echo "✅ ALL REPOSITORIES ACCESSIBLE"
    echo "   Full functionality available including static analysis tools."
fi

# Check Gradle configuration
echo ""
echo "🧪 Testing Gradle configuration..."

# Check AGP version in version catalog
AGP_VERSION=$(grep '^agp = ' gradle/libs.versions.toml | cut -d'"' -f2)
echo "📋 Android Gradle Plugin version: $AGP_VERSION"

# Test basic Gradle functionality
echo "🔄 Testing Gradle build configuration..."

if SKIP_STATIC_ANALYSIS=${SKIP_STATIC_ANALYSIS:-false} timeout 120 ./gradlew help --no-daemon > /tmp/gradle-test.log 2>&1; then
    echo "✅ Gradle configuration is valid"
else
    echo "❌ Gradle configuration has issues"
    echo ""
    echo "🔍 Error details:"
    cat /tmp/gradle-test.log | tail -20
    
    echo ""
    echo "💡 Possible solutions:"
    echo "   1. Ensure internet connectivity to required repositories"
    echo "   2. Configure corporate proxy if needed:"
    echo "      gradle.properties: systemProp.https.proxyHost=proxy.company.com"
    echo "      gradle.properties: systemProp.https.proxyPort=8080"
    echo "   3. Use Android Studio IDE (pre-cached dependencies)"
    echo "   4. Check firewall/allowlist settings"
    
    exit 1
fi

echo ""
echo "✅ ENVIRONMENT SETUP COMPLETE!"

if [ "$REPOS_ACCESSIBLE" = "true" ]; then
    echo ""
    echo "📋 Available development commands:"
    echo "   ./gradlew staticAnalysis    # Run all static analysis tools"
    echo "   ./gradlew formatCode        # Format code with ktlint"
    echo "   ./gradlew checkCodeStyle    # Check code style"
    echo "   ./gradlew detekt           # Run detekt analysis"
    echo "   ./gradlew ktlintCheck      # Check ktlint rules"
    echo "   ./gradlew ktlintFormat     # Auto-format with ktlint"
    echo ""
    echo "📁 Configuration files:"
    echo "   detekt.yml                 # Detekt rules configuration"
    echo "   .editorconfig              # Code style settings"
fi

echo ""
echo "📖 For more information:"
echo "   - Static analysis guide: docs/STATIC_ANALYSIS_GUIDE.md"