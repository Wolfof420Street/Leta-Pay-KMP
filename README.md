<div align="center">

<img src="https://github.com/user-attachments/assets/ab2f5bf9-5b88-4fee-90e9-741e3b3f7a26" alt="LetaPay Logo" width="150" style="margin-right: 20px;" />

<h1>LetaPay</h1>

<p>🚀 Chat-Based Crypto Wallet with AI Agent Orchestration</p>

![Kotlin](https://img.shields.io/badge/Kotlin-7f52ff?style=flat-square&logo=kotlin&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-4c8d3f?style=flat-square&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Jetpack%20Compose%20Multiplatform-000000?style=flat-square&logo=android&logoColor=white)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-DB413D.svg?style=flat)
![badge-web](http://img.shields.io/badge/platform-web-FDD835.svg?style=flat)

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![GitHub license](https://img.shields.io/github/license/Wolfof420Street/Leta-Pay-KMP.svg)](https://github.com/Wolfof420Street/Leta-Pay-KMP/blob/main/LICENSE)
[![Status](https://img.shields.io/badge/Status-MVP%20Development-orange.svg?style=flat-square)](#)

</div>

> **Status**: MVP Development — Core features (wallet connect, chat, payments, swaps, staking) being implemented
>
> **Non-Custodial**: LetaPay never holds user private keys. All transactions are signed by the user's external wallet via WalletConnect.
>
> **Supported Chains**: Ethereum (1), Polygon (137), Base (8453)

## 🌟 Key Features

- **Non-Custodial Wallet**: User owns keys; app never touches private keys via WalletConnect
- **AI Agent Orchestration**: NLP-driven intent parsing with deterministic fast path + LLM fallback
- **Real-Time Chat**: 1:1 direct messages via Firebase Realtime DB with custom wallet-backed auth
- **Cross-Platform**: Native apps on Android, iOS, Desktop (JVM), and Web (Kotlin/JS)
- **Web3 Native**: 
  - Token swaps via Coinbase DEX Aggregator
  - Staking (Lido on Ethereum, AAVE on Polygon)
  - Transaction history via Coinbase CDP Onchain Data API
  - Compliance screening before value transfers
- **Backend API**: Ktor 3.x with structured logging, rate limiting, and idempotent transaction handling
- **Responsive UI**: Compose Multiplatform with shared design system across all platforms

## 🏗️ Architecture

### Cross-Platform Modules
- **cmp-android** - Android app (Compose Multiplatform)
- **cmp-ios** - iOS app (SwiftUI via Compose Native)
- **cmp-desktop** - Desktop app (JVM)
- **cmp-web** - Web app (Kotlin/JS)
- **cmp-shared** - Shared multiplatform logic
- **backend-ktor** - REST API + WebSocket server (Ktor 3.x, JVM 21)

### Core Modules
- **core/ai** - NLP parsing + LLM integration (Koog 0.x)
- **core/network** - API clients (Ktor, Coinbase CDP, Firebase)
- **core/data** - Data layer (repositories, use cases)
- **core/database** - Local persistence (SQLDelight)
- **core/datastore** - User preferences & secure storage
- **core/model** - Domain models (@Serializable Kotlin data classes)
- **core/designsystem** - Shared UI theme & components
- **core/ui** - Common UI utilities

### Feature Modules
- **feature/auth** - Wallet authentication (WalletConnect)
- **feature/wallet** - Portfolio overview + balance
- **feature/chat** - 1:1 messaging with AI agents
- **feature/trade** - Token swaps
- **feature/yield** - Staking/yield farming
- **feature/agent** - AI orchestration & execution
- **feature/profile** - User profile & settings
- **feature/home** - Dashboard & quick actions

## 🚀 Quick Start

### Prerequisites

- **JDK 17+** (Android Studio comes with JDK 17+)
- **Android Studio/IntelliJ IDEA** (2024.2+)
- **Xcode 15+** (macOS, for iOS)
- **Node.js 18+** (for web development)
- **Ktor CLI** (optional, for backend hot reload)

### Clone & Setup

```bash
git clone https://github.com/Wolfof420Street/Leta-Pay-KMP.git
cd Leta-Pay-KMP
```

### Configure Firebase

1. Create Firebase project at [firebase.google.com](https://firebase.google.com)
2. Download `google-services.json` and place in `cmp-android/`
3. Set iOS bundle ID to `org.letapay.app` in Firebase Console

### Build & Run

```bash
# Android
./gradlew cmpAndroid:run

# iOS (requires Xcode)
open cmp-ios/iosApp.xcworkspace
# or
./gradlew iosApp:assembleDebug

# Desktop
./gradlew cmpDesktop:run

# Web
./gradlew wasmJsBrowserDevelopmentRun

# Backend (Ktor)
cd backend-ktor
./gradlew run
```

## 🍎 iOS Deployment

LetaPay includes production-ready iOS deployment infrastructure with support for Firebase App Distribution, TestFlight, and App Store releases.

### Prerequisites

- **macOS** with Xcode 15+ installed
- **Apple Developer Account** ($99/year)
- **Match Repository** (private Git repo for code signing)
- **App Store Connect API Key** (token-based authentication)

### Setup

```bash
# Complete iOS setup with guided configuration
bash scripts/setup_ios_complete.sh

# Alternative: Firebase setup only
bash scripts/setup_firebase.sh

# Alternative: Manage code signing certificates
bash scripts/setup_apn_key.sh
```

### Deployment

Three deployment targets are available:

| Target         | Purpose              | Script                              | Audience |
|----------------|----------------------|-------------------------------------|----------|
| **Firebase**   | Internal testing, QA | `bash scripts/deploy_firebase.sh`   | Testers  |
| **TestFlight** | Beta testing         | `bash scripts/deploy_testflight.sh` | Beta users |
| **App Store**  | Production release   | `bash scripts/deploy_appstore.sh`   | End users |

**Example:**

```bash
# Deploy to Firebase for internal testing
bash scripts/deploy_firebase.sh

# Deploy to TestFlight for beta testing
bash scripts/deploy_testflight.sh

# Deploy to App Store for production (requires double confirmation)
bash scripts/deploy_appstore.sh
```

## 📁 Project Structure

```
Leta-Pay-KMP/
├── cmp-android/               # Android app (Compose Multiplatform)
├── cmp-ios/                   # iOS app (Xcode project)
├── cmp-desktop/               # Desktop app (JVM)
├── cmp-web/                   # Web app (Kotlin/JS)
├── cmp-shared/                # Shared multiplatform UI
├── cmp-navigation/            # Navigation logic
├── backend-ktor/              # REST API & WebSocket server
│
├── core/                       # Core modules
│   ├── ai/                     # NLP parsing + LLM integration
│   ├── network/                # API clients (Coinbase, Firebase, etc.)
│   ├── data/                   # Repositories & use cases
│   ├── database/               # SQLDelight local persistence
│   ├── datastore/              # User preferences & secrets
│   ├── model/                  # Domain models
│   ├── designsystem/           # Shared theme & design tokens
│   └── ui/                     # Common UI utilities
│
├── feature/                    # Feature modules
│   ├── auth/                   # WalletConnect authentication
│   ├── wallet/                 # Portfolio overview
│   ├── chat/                   # 1:1 messaging
│   ├── trade/                  # Token swaps
│   ├── yield/                  # Staking/yield farming
│   ├── agent/                  # AI orchestration
│   ├── profile/                # User profile
│   ├── home/                   # Dashboard
│   └── settings/               # App settings
│
├── core-base/                  # Base platform implementations
├── gradle/                     # Gradle version catalog & plugins
├── scripts/                    # Deployment & utility scripts
├── fastlane/                   # iOS/Android automation config
└── docs/                       # Documentation
```

## 📚 Development Guide

### Build Commands

```bash
# Build all targets
./gradlew build

# Build specific platform
./gradlew cmpAndroid:build         # Android
./gradlew cmpDesktop:build         # Desktop
./gradlew wasmJsBrowserDist         # Web

# Run tests
./gradlew test

# Code quality checks
./gradlew spotlessCheck            # Code formatting
./gradlew detekt                   # Static analysis
./gradlew dependencyGuard          # Dependency validation
```

### Code Formatting

```bash
# Check code formatting
./gradlew spotlessCheck

# Auto-format code
./gradlew spotlessApply
```

### Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and format: `./gradlew spotlessApply`
3. Run checks: `./gradlew spotlessCheck detekt`
4. Commit: `git commit -m "feat(module): description"`
5. Push and create PR

See [Contributing Guidelines](CONTRIBUTING.md) for more details.

## 🛠️ Backend Development

The backend is a **Ktor 3.x** application running on **JVM 21**.

### Start Backend

```bash
cd backend-ktor
./gradlew run
```

The backend will start on `http://localhost:8080`

### Backend Features

- **RESTful API** with proper error handling
- **WebSocket support** for real-time updates
- **Database**: PostgreSQL with Exposed ORM + HikariCP
- **Authentication**: JWT with wallet-backed claims
- **Rate Limiting**: Per-wallet limits on AI endpoints
- **Idempotency**: UUID-based request deduplication

### Backend API Docs

```bash
# Swagger UI will be available at:
# http://localhost:8080/swagger-ui
```

## 🔐 Security

### Key Principles

- **Non-Custodial**: LetaPay never holds user private keys
- **WalletConnect**: All transactions signed by user's external wallet
- **Custom Firebase Tokens**: Wallet-backed authentication (not anonymous)
- **Compliance Screening**: Address screening before value transfers
- **Rate Limiting**: Per-wallet limits prevent abuse
- **Idempotent Transactions**: UUID-based deduplication prevents double-spending

### Secrets Management

⚠️ **Never commit secrets to version control!**

```bash
# View current secrets
./keystore-manager.sh view

# Generate keystores
./keystore-manager.sh generate

# Encode for GitHub Actions
./keystore-manager.sh encode-secrets

# Add to GitHub (requires gh CLI)
./keystore-manager.sh add
```

See [Secrets Management](docs/claude/secrets-management.md) for complete details.

## 🚀 Deployment

### Environments

- **Development**: Local builds on all platforms
- **Staging**: Firebase App Distribution (iOS/Android)
- **Production**: App Store (iOS), Play Store (Android)

### Deployment Pipeline

1. **Android**: Play Store via Fastlane
2. **iOS**: App Store via Fastlane
3. **Web**: GitHub Pages
4. **Desktop**: GitHub Releases

See [Deployment Playbook](docs/claude/deployment-playbook.md) for detailed instructions.

## 📊 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin (1.9.24+) |
| **UI Framework** | Compose Multiplatform |
| **Database (Client)** | SQLDelight |
| **Database (Server)** | PostgreSQL + Exposed |
| **Networking** | Ktor Client/Server |
| **Serialization** | Kotlinx Serialization |
| **DI** | Koin |
| **AI** | Koog (JetBrains) 0.x |
| **Real-time** | Firebase Realtime DB |
| **Analytics** | Firebase Analytics |
| **Code Quality** | Spotless, Detekt |
| **Testing** | Kotest, MockK |

## 📖 Documentation

- [Architecture Overview](docs/claude/patterns.md) - Design patterns & best practices
- [Source Set Hierarchy](docs/SOURCE_SET_HIERARCHY.md) - KMP code sharing guide
- [Troubleshooting](docs/claude/troubleshooting.md) - Common issues & solutions
- [Version Handling](docs/claude/version-handling.md) - Version scheme & semantics
- [GitHub Actions](docs/claude/github-actions-deep-dive.md) - CI/CD workflows
- [Fastlane Configuration](fastlane/CLAUDE.md) - iOS/Android automation

## 📫 Support

- **Issues**: [GitHub Issues](https://github.com/Wolfof420Street/Leta-Pay-KMP/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Wolfof420Street/Leta-Pay-KMP/discussions)

## 📄 License

This project is licensed under the [Mozilla Public License 2.0](LICENSE)
