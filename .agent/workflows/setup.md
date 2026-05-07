---
description: KMP setup workflow: verify toolchain, Gradle quality plugins, platform prerequisites, and CI readiness.
---

# Setup Workflow — Leta-Pay KMP

## Step 1: Baseline Prerequisites
Verify:
- Java and Gradle wrapper availability
- Android SDK/toolchain
- Xcode + CocoaPods for iOS
- Node/runtime prerequisites for web build

## Step 2: Project Bootstrap
Run:
- ./setup-project.sh (if first-time setup)
- ./gradlew tasks (sanity check)

## Step 3: Quality Toolchain Check
Run:
- ./gradlew spotlessApply
- ./gradlew spotlessCheck
- ./gradlew detekt
- ./gradlew dependencyGuard

## Step 4: Build Smoke Checks
Run representative builds:
- ./gradlew :cmp-android:assembleDebug
- ./gradlew :cmp-shared:build
- ./gradlew jsBrowserDistribution

## Step 5: Security and Secrets Hygiene
- ensure forbidden secret files are not staged
- validate local env follows security-policy.yaml

## Step 6: Setup Report
Provide:
- installed/missing prerequisites
- gate status
- platform readiness matrix
- recommended next actions
