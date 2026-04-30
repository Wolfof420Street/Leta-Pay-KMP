#!/usr/bin/env bash
set -euo pipefail

# Run Spotless across all modules in one pass.
# --continue ensures Gradle reports all violations instead of stopping at first failure.
./gradlew --no-daemon spotlessApply --continue
./gradlew --no-daemon spotlessCheck --continue
