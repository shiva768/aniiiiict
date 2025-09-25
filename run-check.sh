#!/bin/bash
# Script to run gradle check with required environment variables
export ANNICT_CLIENT_ID="dummy_client_id"
export ANNICT_CLIENT_SECRET="dummy_client_secret"
export MAL_CLIENT_ID="dummy_mal_client_id"

echo "Running gradle check with environment variables set..."
./gradlew check --no-daemon