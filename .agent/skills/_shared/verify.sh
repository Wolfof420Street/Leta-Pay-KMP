#!/bin/bash
# verify.sh - Shared verification script for KMP agent skills
# Run after implementation to validate mandatory quality gates.

set -euo pipefail

if [ ! -f "gradlew" ]; then
  echo "ERROR: gradlew not found. Run from repository root."
  exit 1
fi

echo "Running KMP quality gates..."

echo "1) Spotless check"
./gradlew spotlessCheck

echo "2) Detekt"
./gradlew detekt

echo "3) Tests"
./gradlew test

echo "4) Dependency guard"
./gradlew dependencyGuard

echo "Verification complete."
