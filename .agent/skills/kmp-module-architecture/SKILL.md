---
name: kmp-module-architecture
description: "KMP module layout specialist. Use when: designing module structure, managing dependencies between shared/feature/platform modules, or solving build/compile issues related to module organization."
---

# KMP Module Architecture - Dependency Management

## When to Use
- Designing new feature modules within the structure
- Resolving circular dependency issues
- Organizing expect/actual declarations
- Setting up gradle dependencies between modules
- Determining where to place new code (shared vs feature vs platform)

## When NOT to Use
- Business logic implementation → use Shared KMP Core skill
- Platform-specific code → use KMP Platform Specifics skill
- UI/Compose code → use KMP UI Compose skill

## Module Structure Overview

```
Leta-Pay-KMP/
├── cmp-android/              # Android app entry point
├── cmp-ios/                  # iOS Xcode project  
├── cmp-desktop/              # Desktop JVM app
├── cmp-web/                  # Web/JS app
│
├── cmp-shared/               # Main KMP module
│   └── src/
│       ├── commonMain/       # Shared code for ALL platforms
│       ├── androidMain/      # Android-specific (expect/actual impl)
│       ├── iosMain/          # iOS-specific (expect/actual impl)
│       ├── desktopMain/      # Desktop/JVM-specific (expect/actual impl)
│       ├── jsMain/           # Web/JS-specific (expect/actual impl)
│       └── commonTest/       # Tests (all platforms)
│
├── feature/                  # Feature modules (optional, for large apps)
│   ├── auth/
│   ├── wallet/
│   ├── chat/
│   ├── trade/
│   └── yield/
│
├── core/                     # Core layers
│   ├── core-network/         # Ktor, Firebase
│   ├── core-database/        # SQLDelight
│   ├── core-storage/         # Secure storage (expect/actual)
│   ├── core-di/              # Koin modules
│   └── core-common/          # Extensions, constants
│
├── core-base/                # Multiplatform base (usually empty in KMP)
│
└── scripts/                  # Gradle conventions
    └── gradle-convention/
        ├── kotlin-multiplatform.gradle.kts
        ├── compose-multiplatform.gradle.kts
        └── koin-di.gradle.kts
```

## Detailed Layer Organization

### Layer 1: Presentation (UI Layer)

**Location**: `cmp-shared/src/commonMain/kotlin/ui/`

**Responsibility**: Compose Multiplatform screens, components, view models

**Dependencies**:
- ✅ Feature layer (repositories, use cases)
- ✅ Theme, navigation, design tokens
- ❌ Network layer (indirect via repositories)
- ❌ Database layer (indirect via repositories)

```
ui/
├── screens/
│   ├── ChatScreen.kt
│   ├── WalletScreen.kt
│   ├── TradeScreen.kt
│   ├── YieldScreen.kt
│   └── AuthScreen.kt
├── viewmodel/
│   ├── ChatViewModel.kt
│   ├── WalletViewModel.kt
│   ├── TradeViewModel.kt
│   ├── YieldViewModel.kt
│   └── AuthViewModel.kt
├── components/
│   ├── TokenCard.kt
│   ├── TransactionItem.kt
│   ├── ErrorCard.kt
│   ├── LoadingSkeletons.kt
│   └── ChatBubble.kt
├── navigation/
│   ├── Routes.kt
│   ├── NavHost.kt
│   └── Navigation.kt
└── theme/
    ├── Theme.kt
    ├── Color.kt
    ├── Typography.kt
    └── Shapes.kt
```

### Layer 2: Feature/Domain (Business Logic Layer)

**Location**: `cmp-shared/src/commonMain/kotlin/feature/`

**Responsibility**: Repositories, use cases, domain models, state holders

**Dependencies**:
- ✅ Data layer (repositories for persistence)
- ✅ Network layer (HTTP client)
- ✅ Platform layer (for expect/actual)
- ❌ UI layer (no UI code)

```
feature/
├── auth/
│   ├── AuthRepository.kt
│   ├── AuthService.kt
│   ├── AuthViewModel.kt   # or StateHolder
│   └── models/
│       ├── AuthSession.kt
│       └── AuthState.kt
├── wallet/
│   ├── WalletRepository.kt
│   ├── GetBalanceUseCase.kt
│   ├── RefreshBalancesUseCase.kt
│   └── models/
│       ├── Balance.kt
│       └── Token.kt
├── chat/
│   ├── ChatRepository.kt
│   ├── SendMessageUseCase.kt
│   ├── GetMessagesUseCase.kt
│   └── models/
│       ├── ChatMessage.kt
│       └── ChatRoom.kt
├── trade/
│   ├── TradeRepository.kt
│   ├── GetSwapQuoteUseCase.kt
│   ├── ExecuteSwapUseCase.kt
│   └── models/
│       ├── SwapQuote.kt
│       └── SwapOrder.kt
└── yield/
    ├── YieldRepository.kt
    ├── GetStakingOpportunitiesUseCase.kt
    ├── StakeUseCase.kt
    └── models/
        ├── StakingOpportunity.kt
        └── StakingPosition.kt
```

### Layer 3: Data (Persistence & Sync)

**Location**: `cmp-shared/src/commonMain/kotlin/data/`

**Responsibility**: SQL queries, cache managers, offline queue

**Dependencies**:
- ✅ Database (SQLDelight)
- ✅ Firebase (read-only refs)
- ❌ UI layer
- ❌ Repository interface implementations (belong in feature layer)

```
data/
├── database/
│   ├── Database.kt
│   ├── ChatMessageQueries.kt
│   ├── TransactionQueries.kt
│   ├── TokenMetadataQueries.kt
│   ├── StakingPositionQueries.kt
│   └── schema.sqldelight
├── cache/
│   ├── CacheManager.kt
│   ├── TokenMetadataCache.kt
│   └── PriceCache.kt
└── sync/
    ├── OfflineQueue.kt
    ├── SyncManager.kt
    └── ConflictResolver.kt
```

### Layer 4: Core (Shared Infrastructure)

**Location**: `cmp-shared/src/commonMain/kotlin/core/`

**Responsibility**: HTTP client, Firebase setup, error handling, typed models

**Dependencies**:
- ✅ Platform layer (for expect/actual)
- ❌ Any business logic

```
core/
├── model/
│   ├── ChainId.kt
│   ├── WalletAddress.kt
│   ├── TxHash.kt
│   ├── AssetId.kt
│   ├── Token.kt
│   └── TransactionStatus.kt
├── error/
│   └── AppError.kt
├── resource/
│   └── Resource.kt
├── network/
│   ├── HttpClientFactory.kt
│   ├── AuthService.kt
│   ├── AuthInterceptor.kt
│   ├── ErrorInterceptor.kt
│   ├── RetryInterceptor.kt
│   └── FirebaseConfig.kt
├── database/
│   └── Database.kt (SQLDelight wrapper)
├── storage/
│   └── expect SecureStorage
├── notification/
│   └── expect PushNotificationManager
├── dispatcher/
│   └── expect DispatcherProvider
├── di/
│   ├── config/
│   │   ├── AppConfig.kt
│   │   ├── DevConfig.kt
│   │   └── ProdConfig.kt
│   └── AppModule.kt
└── common/
    ├── Extensions.kt
    ├── Constants.kt
    ├── Formatters.kt
    └── Validators.kt
```

### Layer 5: Platform (Expect/Actual)

**Expect Declarations**: `cmp-shared/src/commonMain/kotlin/platform/`

**Implementations**:
- Android: `cmp-shared/src/androidMain/kotlin/platform/`
- iOS: `cmp-shared/src/iosMain/kotlin/platform/`
- Desktop: `cmp-shared/src/desktopMain/kotlin/platform/`
- Web: `cmp-shared/src/jsMain/kotlin/platform/`

```
platform/
├── SecureStorage.kt (expect)
├── DispatcherProvider.kt (expect)
├── PushNotificationManager.kt (expect)
├── LoggerProvider.kt (expect)
├── UUIDGenerator.kt (expect)
└── PlatformContext.kt (expect)

// androidMain/kotlin/platform/
├── SecureStorage.kt (actual - EncryptedSharedPreferences)
├── DispatcherProvider.kt (actual)
├── PushNotificationManager.kt (actual - FCM)
├── LoggerProvider.kt (actual)
├── UUIDGenerator.kt (actual)
└── PlatformContext.kt (actual)

// (similar for iosMain, desktopMain, jsMain)
```

## Dependency Graph (Acyclic)

```
UI Layer
    ↓ (depends on)
Feature Layer
    ↓ (depends on)
Data Layer
    ↓ (depends on)
Core Layer
    ↓ (depends on)
Platform Layer
    ↓ (depends on)
Kotlin Multiplatform
```

**RULE**: Never import from above. Only import from layers below.

### Valid Imports
```kotlin
// UI → Feature ✅
import feature.chat.ChatRepository

// Feature → Core ✅
import core.model.WalletAddress
import core.error.AppError

// Core → Platform ✅
import platform.SecureStorage

// Feature → Feature ❌  (cross-feature imports)
// Data → UI ❌  (importing upward)
```

## Module Declaration (build.gradle.kts)

```kotlin
// cmp-shared/build.gradle.kts
plugins {
    id("kmp-multiplatform")
    id("kmp-di")
}

kotlin {
    jvm()
    ios()
    androidTarget()
    js(IR) { browser() }
    macosX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core dependencies
                implementation("io.ktor:ktor-client-core:2.3.0")
                implementation("io.insert-koin:koin-core:3.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
                implementation("app.cash.sqldelight:runtime:2.0.0")
                implementation("io.github.aakira:napier:2.6.1")
                
                // Firebase only on supported platforms
                implementation("com.google.firebase:firebase-database-ktx:20.0.6")
                implementation("com.google.firebase:firebase-auth-ktx:22.1.0")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.0")
                implementation("io.mockk:mockk:1.13.5")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("com.google.firebase:firebase-messaging:23.2.1")
            }
        }
        
        val iosMain by getting {
            // Handled via CocoaPods
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-java:2.3.0")
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.0")
            }
        }
    }
}
```

## Feature Module Pattern (For Large Projects)

If splitting into separate feature modules:

```kotlin
// feature/auth/build.gradle.kts
plugins {
    id("kmp-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Only depend on CORE, never on other features
                implementation(project(":cmp-shared:core"))
                
                // NOT on other features:
                // ❌ implementation(project(":feature:wallet"))
            }
        }
    }
}

// feature/wallet/build.gradle.kts
// Can depend on feature/auth if needed (unidirectional)
dependencies {
    implementation(project(":feature:auth"))
}

// BUT feature/auth cannot depend on feature/wallet
```

**Cross-Feature Rule**: Use `SharedFlow` or `EventBus` pattern for communication, never direct imports.

## Where to Place New Code

### "I need to handle wallet balances"
- **Value class**: `core/model/Balance.kt`
- **Repository interface**: `feature/wallet/WalletRepository.kt`
- **Repository implementation**: `feature/wallet/impl/WalletRepositoryImpl.kt`
- **Use case**: `feature/wallet/GetBalanceUseCase.kt`
- **View model**: `ui/viewmodel/WalletViewModel.kt`
- **Screen**: `ui/screens/WalletScreen.kt`
- **Database schema**: `data/database/schema.sqldelight`
- **Queries**: `data/database/BalanceQueries.kt` (SQLDelight-generated)

### "I need Firebase Realtime for chat"
- **Data model**: `core/model/ChatMessage.kt`
- **Firebase entity**: `feature/chat/FirebaseChatMessage.kt` (if different shape)
- **Repository**: `feature/chat/ChatRepository.kt`
- **Firebase config**: `core/network/FirebaseConfig.kt`
- **Local schema**: `data/database/chat_messages.sqldelight`
- **Sync manager**: `data/sync/ChatSyncManager.kt`

### "I need to add a new platform-specific feature"
- **Expect**: `core/platform/FeatureName.kt` (in commonMain)
- **Implement**: `androidMain/kotlin/platform/FeatureName.kt`
- **Implement**: `iosMain/kotlin/platform/FeatureName.kt`
- **Implement**: `desktopMain/kotlin/platform/FeatureName.kt`
- **Implement**: `jsMain/kotlin/platform/FeatureName.kt`
- **Use in Feature**: `feature/*/FeatureRepository.kt` calls expect interface

## Common Mistakes

❌ **Putting UI logic in core**: Core should be platform-agnostic
❌ **Importing upward** (feature importing from UI): Creates circular deps
❌ **Expect in feature module**: Expect/actual must be in core or separate module
❌ **Platform code in commonMain**: Should be in androidMain, iosMain, etc.
❌ **Feature-to-feature imports**: Use EventBus or shared flows instead
❌ **Tests in src instead of test**: Always `src/{platform}Test/`

## Verification Checklist

- ✅ No circular dependencies (use `./gradlew projects` to verify)
- ✅ All expect declarations have 4+ implementations (android, ios, desktop, web)
- ✅ No platform-specific code in `commonMain`
- ✅ No UI code in `feature` layer
- ✅ No business logic in `ui` layer (only state management)
- ✅ Database schema only in `data` layer
- ✅ Repositories only in `feature` layer
- ✅ HTTP client only in `core/network`
- ✅ All exceptions map to `AppError` sealed class

## Gradle Dependency Audit

```bash
# View all dependencies
./gradlew dependencies

# View KMP-specific sourcesets
./gradlew projects

# Build and verify no circular deps
./gradlew checkDependencies

# View dependency conflicts
./gradlew dependencyInsight --dependency ktor-client-core
```

## References
- Execution steps: `resources/execution-protocol.md`
- Dependency matrix: `resources/dependency-matrix.md`
- Gradle conventions: `resources/gradle-setup.md`
- Checklist: `resources/checklist.md`
