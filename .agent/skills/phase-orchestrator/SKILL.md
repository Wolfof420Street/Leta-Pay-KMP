---
name: phase-orchestrator
description: "Phase-based development executor. Use when: planning phase-based work (Phase 1-9), managing deliverables, tracking phase completeness, or verifying gate criteria before moving to next phase."
---

# Phase Orchestrator - Structured Development

## When to Use
- Planning work for a specific phase (1-9 from plan.md)
- Executing deliverables for the current phase
- Verifying gate criteria before moving to next phase
- Breaking down phase tasks into sprint work
- Tracking phase progress and blockers

## When NOT to Use
- Individual file changes → use skill matching the specific domain
- Debugging complex issues → use Debug or Platform Specifics skills
- General questions → ask Copilot directly

## Phase Overview

| Phase | Focus | Duration | Gate Criteria |
|-------|-------|----------|---------------|
| **1** | Setup, DI, Core Network, Root Auth | Days 1-2 | All 4 platforms build; Koin resolves; Feature flags work |
| **2** | WalletConnect & Session Management | Days 2-4 | SessionToken issued; balances persist; session persists on restart |
| **3** | Chat Scaffolding & Firebase | Days 3-5 | Chat UI works; Firebase connected; offline queue ready |
| **4** | Transaction Intent & Send Payment | Days 4-7 | Build/broadcast separation works; WalletConnect signs; status updates |
| **5** | Trading/Swaps | Days 6-8 | Quote fetches; quotes update real-time; swaps confirmed |
| **6** | Staking/Yield | Days 7-9 | Stake UI works; positions track; rewards visible |
| **7** | Push Notifications & Error Handling | Days 8-10 | Notifications work; errors graceful and recoverable |
| **8** | Polish, Testing, Performance | Days 9-11 | Skeletons, animations; E2E on all 4 platforms; testnet verified |
| **9** | Platform-Specific Builds & Deployment | Days 11-13 | Beta live on all platforms; staged rollout ready |

## Phase 1: Setup, DI, Core Network, Root Auth

### Objectives
1. Initialize KMP project gradle structure
2. Set up Koin DI with all layers
3. Create Ktor HTTP client with interceptors
4. Implement root navigation shell
5. Bootstrap AppStateHolder

### Deliverables

#### 1. Gradle Convention Plugins
```kotlin
// gradle/convention/kotlin-multiplatform.gradle.kts
plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
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
                implementation("io.ktor:ktor-client-core:2.3.0")
                implementation("io.insert-koin:koin-core:3.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
            }
        }
    }
}
```

#### 2. Environment Config
```kotlin
// commonMain/kotlin/di/config/AppConfig.kt
interface AppConfig {
    val apiBaseUrl: String
    val firebaseProjectId: String
    val isDebug: Boolean
    val enableYield: Boolean
    val enableSwap: Boolean
}

object DevConfig : AppConfig {
    override val apiBaseUrl = "http://localhost:3000"
    override val firebaseProjectId = "leta-pay-dev"
    override val isDebug = true
    override val enableYield = true
    override val enableSwap = true
}

object ProdConfig : AppConfig {
    override val apiBaseUrl = "https://api.letapay.com"
    override val firebaseProjectId = "leta-pay-prod"
    override val isDebug = false
    override val enableYield = true
    override val enableSwap = true
}
```

#### 3. Koin DI Setup
```kotlin
// commonMain/kotlin/di/AppModule.kt
val appModule = module {
    // Configuration
    single<AppConfig> { 
        if (BuildConfig.DEBUG) DevConfig else ProdConfig 
    }
    
    // Platform layer
    single { SecureStorage }
    single { DispatcherProvider }
    single { PushNotificationManager }
    
    // Networking
    single { HttpClientFactory.create(get()) }
    single { AuthService(get(), get()) }
    
    // Database
    single { Database() }
    
    // Firebase
    single { FirebaseConfig() }
    
    // Repositories
    single { WalletRepository(get(), get(), get()) }
    single { AuthRepository(get(), get()) }
    
    // State
    single { AppStateHolder(get(), get()} }
    
    // View models
    viewModel { ChatViewModel(get()) }
    viewModel { WalletViewModel(get()) }
}
```

#### 4. Root Navigation
```kotlin
// commonMain/kotlin/ui/navigation/RootNavHost.kt
@Composable
fun RootNavHost(
    appState: AppStateHolder
) {
    val isAuthenticated by appState.isAuthenticated.collectAsState()
    
    val navController = rememberNavController()
    
    if (isAuthenticated) {
        MainNavHost(navController)
    } else {
        AuthNavHost(navController, onAuthSuccess = {
            // Navigate to main
        })
    }
}

@Composable
fun MainNavHost(navController: NavController) {
    Scaffold(
        bottomBar = { BottomNavigation(navController) }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Chat.path,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable(Route.Chat.path) { ChatScreen() }
            composable(Route.Wallet.path) { WalletScreen() }
            composable(Route.Trade.path) { TradeScreen() }
            composable(Route.Yield.path) { YieldScreen() }
            composable(Route.Account.path) { AccountScreen() }
        }
    }
}
```

#### 5. Feature Flags
```kotlin
// commonMain/kotlin/common/FeatureFlags.kt
data class FeatureFlags(
    val yieldEnabled: Boolean = true,
    val swapEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = false,  // Phase 7
    val biometricEnabled: Boolean = false  // Post-MVP
)

// Usage in UI
if (featureFlags.yieldEnabled) {
    navigateTo(Route.Yield)
}
```

### Gate Criteria - All MUST PASS

- ✅ `./gradlew build` succeeds on Android, iOS, Desktop, Web
- ✅ Koin DI graph resolves without errors: `koin.verify(appModule)`
- ✅ App launches to main screen with placeholder content
- ✅ Bottom nav switches between 5 screens
- ✅ Feature flags toggle screens on/off without crashes
- ✅ Logging via Napier shows all state changes at DEBUG level
- ✅ No platform-specific code in commonMain
- ✅ All expect/actual declarations compile

### Common Blockers

| Blocker | Fix |
|---------|-----|
| Gradle version mismatch | Pin Kotlin version in `gradle/convention/` |
| Koin module conflicts | Ensure no duplicate `single<Type>()` declarations |
| iOS CocoaPods issues | Run `pod install` in `cmp-ios/` after adding dependencies |
| Web build fails | Use `jsCompilations` instead of `jsBrowserDistribution` for quick test |

---

## Phase 2: WalletConnect & Session Management

### Objectives
1. Implement WalletConnect integration
2. Create session lifecycle (request nonce → sign → verify)
3. Persist sessionToken + refreshToken
4. Bootstrap app on launch with existing session
5. Implement token refresh logic

### Deliverables

**Gate Criteria**:
- ✅ Tap "Connect Wallet" → WalletConnect popup opens
- ✅ Sign message in external wallet → app receives wallet address
- ✅ Backend issues sessionToken + firebaseToken
- ✅ Close app, reopen → still authenticated with same wallet
- ✅ Balances visible immediately after connect
- ✅ Session expires gracefully (refresh token or re-auth)

---

## Phase 3: Chat Scaffolding & Firebase

### Objectives
1. Set up Firebase Realtime Database
2. Implement custom authentication (wallet-backed)
3. Design chat data schema (SQLDelight + Firebase)
4. Create offline message queue
5. Build basic ChatScreen UI

### Deliverables

**Gate Criteria**:
- ✅ Firebase auth using custom token (wallet address as UID)
- ✅ Can read/write to Firebase Realtime DB
- ✅ SQLDelight stores messages locally
- ✅ Pending messages queue while offline
- ✅ ChatScreen displays messages (local + synced from Firebase)
- ✅ Message sends to Firebase and local DB

---

## Phase 4: Transaction Intent & Send Payment

### Objectives
1. Design transaction build vs broadcast endpoints
2. Implement recipient validation
3. Create transaction preview screen
4. Integrate WalletConnect signing
5. Handle transaction status lifecycle

### Deliverables

**Gate Criteria**:
- ✅ Enter recipient + amount → backend estimates gas
- ✅ Tap "Send" → WalletConnect popup for signature
- ✅ Sign → broadcast to Vercel backend
- ✅ Chain confirms → status updates in UI
- ✅ Failed transaction shows error message
- ✅ Transaction appears in chat history

---

## Phase 5: Trading/Swaps

### Objectives
1. Implement quote fetching with auto-refresh
2. Build swap confirmation flow
3. Implement quote expiry timer
4. Handle slippage validation
5. Integrate swap execution

### Deliverables

**Gate Criteria**:
- ✅ Quote fetches on input change (debounced)
- ✅ Timer counts down "Quote expiring in 45s"
- ✅ Manual confirm → broadcast swap tx
- ✅ Swap confirmed → appears in portfolio
- ✅ Manual slippage adjustment works
- ✅ Exceeding max slippage shows error

---

## Phase 6: Staking/Yield

### Objectives
1. Fetch available staking opportunities
2. Implement stake building (estimate gas + APY)
3. Create position tracking UI
4. Handle reward claiming
5. Support one provider per chain

### Deliverables

**Gate Criteria**:
- ✅ Yield screen shows: Ethereum (Lido) + Polygon (AAVE)
- ✅ Tap "Stake" → confirm APY + gas estimate
- ✅ WalletConnect sign → broadcast stake tx
- ✅ Position appears in "My Positions"
- ✅ Rewards visible and claimable
- ✅ MVP release criterion: if Base provider not ready, launch with Ethereum + Polygon only

---

## Phase 7: Push Notifications & Error Handling

### Objectives
1. Implement platform push handlers
2. Create notification factory (incoming payment, swap confirmed, reward claimable)
3. Implement deep linking to screens/details
4. Comprehensive error handling + recovery
5. Retry logic for transient failures

### Deliverables

**Gate Criteria**:
- ✅ Send test notification → receives on all 4 platforms
- ✅ Notification tap → navigates to relevant screen
- ✅ Network down → app uses cached state + retry on reconnect
- ✅ Insufficient balance → error message, send prevented
- ✅ Quote expired → auto-refresh, ask user to confirm
- ✅ Blockchain rejected → show reason to user

---

## Phase 8: Polish, Testing, Performance

### Objectives
1. Add loading skeletons on all screens
2. Implement animations and transitions
3. Add haptic feedback
4. E2E testing on testnet
5. Performance profiling (< 60fps)

### Deliverables

**Gate Criteria**:
- ✅ All screens have loading states with skeleton animations
- ✅ 60fps on real devices (Android, iPhone, macOS, Windows)
- ✅ E2E journey on testnet for all 6 features
- ✅ Unit tests for critical business logic (>70% coverage)
- ✅ Integration tests for auth + chat sync
- ✅ No crashes on any platform

---

## Phase 9: Platform-Specific Builds & Deployment

### Objectives
1. Android: Build Play Store release APK
2. iOS: TestFlight beta build
3. macOS: App Store release
4. Desktop: Installers for Windows/macOS/Linux
5. Web: Deploy to Vercel
6. Staged rollout (internal → beta → 10% → 100%)

### Deliverables

**Gate Criteria**:
- ✅ Android internal testing on Firebase App Distribution
- ✅ iOS TestFlight beta open to testers
- ✅ Desktop installers downloadable and installable
- ✅ Web version accessible at letapay.com
- ✅ Rollout plan documented (no 100% until 3 days stable)

---

## Execution Checklist Template

Use this for each phase:

```markdown
## Phase X: [Name]

### Pre-Execution
- [ ] Phase X-1 gate criteria all passed
- [ ] Resources allocated (time, team)
- [ ] Dependencies resolved (APIs, designs, data)

### Execution
- [ ] Deliverable 1 complete + tested
- [ ] Deliverable 2 complete + tested
- [ ] Deliverable 3 complete + tested

### Gate Verification
- [ ] Criterion 1 passes
- [ ] Criterion 2 passes
- [ ] Criterion 3 passes
- [ ] All manual E2E tested on all 4 platforms

### Go/No-Go Decision
**Decision**: GO / NO-GO (with reasons)
**Blockers** (if NO-GO):
- Blocker 1
- Blocker 2

**Next Phase Date**: [DATE]
```

## Cross-Phase Dependencies

```
Phase 1 (DI, Network) ──→ Phase 2 (WalletConnect)
                            ↓
Phase 3 (Chat) ──→ Phase 4 (Send Payment) ──→ Phase 5 (Swaps)
                            ↓
                    Phase 6 (Staking)
                            ↓
                    Phase 7 (Push + Error)
                            ↓
                    Phase 8 (Polish)
                            ↓
                    Phase 9 (Deploy)
```

## Parallel Work Opportunities

- **Phase 2 & 3 in parallel**: Session mgmt + chat scaffolding don't depend on each other
- **Phase 4, 5, 6 can start phase 2 completion**: Transaction intent doesn't depend on chat

## When to Slip/Add Scope

**Do NOT slip Phase 1**: It unblocks everything.
**Can slip Phase 6**: If Base staking provider isn't production-ready, launch with Ethereum + Polygon, add Base post-MVP.
**Add scope ONLY after Phase X **completion** if Phase X+1 is blocked for other reasons.**

## Reporting

Update `plan.md` after each phase:

```markdown
## PHASE EXECUTION LOG

**Phase 1**: ✅ COMPLETE (Actual: 1.5 days vs planned 2)
**Phase 2**: ✅ COMPLETE (Actual: 3 days vs planned 2) - WalletConnect testing took longer
**Phase 3**: 🔄 IN PROGRESS (Completed 60%)
**Phase 4**: ⏳ BLOCKED - Waiting for Phase 3 complete
```

## References
- Individual phase execution protocols: `resources/phase-[1-9]-execution.md`
- Checklist: `resources/checklist.md`
- Dependency matrix: `resources/dependencies.md`
- Rollback procedure (if phase fails): `resources/rollback.md`
