# Setup Guide

Follow these steps to build and run the Leta Pay project from a clean checkout.

## Prerequisites
- **JDK 21** or higher.
- **Android Studio** (Koala or later) with KMP plugins.
- **Xcode** (for iOS/macOS targets).
- **Node.js 18+** (for agentkit-sidecar).
- **Docker** (for running backend infrastructure).

## 1. Initial Project Setup

Run the master setup script:
```bash
./setup-project.sh
```

## 2. Secrets & Configuration

Generate local keystores and env templates:
```bash
./keystore-manager.sh generate
cp secrets.env.template .env
```
Fill in the `.env` file with required API keys (Coinbase, OpenAI, etc.).

## 3. Building & Running Targets

### Android
```bash
./gradlew :cmp-android:assembleDebug
```
Or run from Android Studio using the `cmp-android` configuration.

### Desktop (JVM)
```bash
./gradlew :cmp-desktop:run
```

### Web (Kotlin/JS)
```bash
./gradlew :cmp-web:jsBrowserDevelopmentRun --continuous
```

### iOS
1. Open `cmp-ios/LetaPay.xcworkspace` in Xcode.
2. Select target and run.

### Backend (Ktor)
```bash
./gradlew :backend-ktor:run
```
Alternatively, use Docker:
```bash
docker-compose up backend
```

## 4. Code Quality Checks

Run all linting and formatting checks:
```bash
./gradlew check spotlessCheck detekt dependencyGuard
```

To automatically fix formatting issues:
```bash
./gradlew spotlessApply
```
