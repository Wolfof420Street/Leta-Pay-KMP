---
name: shared-kmp-core
description: "KMP shared business logic specialist. Use when: implementing domain models, error handling, Resource wrappers, expect/actual patterns, typed value classes, or core domain logic that runs on all platforms (Android, iOS, Desktop, Web)."
---

# Shared KMP Core - Multiplatform Business Logic

## When to Use
- Building domain models and value classes (ChainId, WalletAddress, TxHash, AssetId)
- Implementing error handling (AppError sealed hierarchy)
- Creating Resource<T> wrappers for async states
- Writing expect/actual declarations for platform-specific behavior
- Building repositories and use cases that target `commonMain`
- Defining typed primitives to avoid raw strings/numbers

## When NOT to Use
- Platform-specific UI → use KMP UI Compose skill
- Platform-specific implementation details → use KMP Platform Specifics skill
- Testing logic → use KMP Testing skill
- HTTP client setup → use Firebase & Realtime skill

## Core Rules for Shared Code

### 1. Type Safety (Avoid Stringly Typed Code)
```kotlin
// ✅ GOOD - Typed value classes
@JvmInline
value class ChainId(val value: Long) {
    companion object {
        val Ethereum = ChainId(1L)
        val Polygon = ChainId(137L)
        val Base = ChainId(8453L)
    }
}

@JvmInline
value class WalletAddress(val value: String) {
    init {
        require(value.matches(Regex("^0x[a-fA-F0-9]{40}$"))) { "Invalid address" }
    }
}

// ❌ WRONG - Raw strings
val chain = "ethereum"
val address = "0x1234..."
```

### 2. Error Handling - Sealed Hierarchy
```kotlin
sealed class AppError(open val message: String = "") : Exception(message) {
    data class Network(val cause: IOException) : AppError("Network error")
    data class Unauthorized(val reason: String) : AppError(reason)
    data class InsufficientBalance(val required: BigDecimal, val actual: BigDecimal) : 
        AppError("Insufficient: need $required, have $actual")
    data class QuoteExpired(val expiresAt: Long) : AppError("Quote expired")
    data class Validation(val field: String, val reason: String) : AppError("$field: $reason")
    data class ChainRejected(val reason: String, val code: Int) : AppError(reason)
    data class Unexpected(val error: Throwable) : AppError(error.message ?: "Unknown")
}
```

### 3. Resource Wrapper (All Async States)
```kotlin
sealed class Resource<T> {
    data class Loading<T>(val data: T? = null) : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val error: AppError, val data: T? = null) : Resource<T>()
}

// Usage in repo
fun getBalance(address: WalletAddress): Flow<Resource<Balance>> = flow {
    emit(Resource.Loading())
    try {
        val balance = httpClient.get("/balance/$address")
        emit(Resource.Success(balance))
    } catch (e: Exception) {
        emit(Resource.Error(AppError.Network(e)))
    }
}
```

### 4. Expect/Actual Pattern - Platform-Specific Interfaces
Place in `src/commonMain/` with interface, then `src/androidMain/`, `src/iosMain/`, etc. with implementations.

```kotlin
// commonMain/kotlin/platform/SecureStorage.kt
expect object SecureStorage {
    suspend fun store(key: String, value: String)
    suspend fun retrieve(key: String): String?
    suspend fun delete(key: String)
}

// androidMain/kotlin/platform/SecureStorage.kt
actual object SecureStorage {
    // Android: EncryptedSharedPreferences
}

// iosMain/kotlin/platform/SecureStorage.kt
actual object SecureStorage {
    // iOS: Keychain
}
```

### 5. Module Structure
```
shared/
├── src/
│   ├── commonMain/kotlin/
│   │   ├── model/              # Domain models, value classes
│   │   ├── error/              # AppError sealed hierarchy
│   │   ├── resource/           # Resource<T> wrapper
│   │   ├── network/            # Ktor client, API interfaces
│   │   ├── repository/         # Repository interfaces (data layer)
│   │   ├── usecase/            # Business logic (use cases)
│   │   ├── platform/           # Expect declarations
│   │   ├── di/                 # Koin module setup
│   │   └── common/             # Extensions, constants, formatters
│   ├── androidMain/kotlin/platform/   # Android SecureStorage, DispatcherProvider
│   ├── iosMain/kotlin/platform/       # iOS Keychain, DispatcherProvider
│   ├── desktopMain/kotlin/platform/   # Desktop encrypted storage
│   └── jsMain/kotlin/platform/        # Web IndexedDB, Web Push
```

### 6. Repository Pattern (Clean Architecture)
```kotlin
// Interface in commonMain (defines contract)
interface WalletRepository {
    suspend fun getBalance(address: WalletAddress): Resource<Balance>
    suspend fun refreshBalances(): Resource<List<Balance>>
}

// Implementation handles platform differences
class WalletRepositoryImpl(
    private val httpClient: HttpClient,
    private val database: Database,
    private val secureStorage: SecureStorage
) : WalletRepository {
    // Implementation uses SecureStorage.get() (expect/actual internally handles platform)
}
```

### 7. Transaction Status Enum (Centralized State Machine)
```kotlin
enum class TransactionStatus {
    Draft,                      // User editing amount/recipient
    EstimatingGas,             // Calling backend for gas
    AwaitingWalletApproval,    // WalletConnect popup shown
    Signed,                     // User approved signature
    Broadcasting,              // Posting signed tx to backend
    Submitted,                 // Have tx hash, listening to chain
    Confirmed,                 // N confirmations reached
    Failed,                    // Error from chain
    Cancelled                  // User dismissed before broadcast
}
```

## File Location Rules

- **Value Classes & Models**: `src/commonMain/kotlin/model/`
- **Error Types**: `src/commonMain/kotlin/error/`
- **Repositories (interfaces)**: `src/commonMain/kotlin/repository/`
- **Expect/Actual declarations**: `src/commonMain/kotlin/platform/` + `src/androidMain/`, `src/iosMain/`, `src/desktopMain/`, `src/jsMain/`
- **Koin modules**: `src/commonMain/kotlin/di/`

## Verifying Correct Architecture

1. **No platform-specific code in commonMain** - Use expect/actual for platform differences
2. **All models are immutable data classes** - No mutable state
3. **Error handling is exhaustive** - All error paths map to AppError subtype
4. **Resources are consistent** - All async operations return Resource<T>, never raw T
5. **Value classes validate on construction** - No validation in business logic
6. **Repositories define contracts** - Implementation in concrete modules

## Common Patterns in Leta Pay

**Wallet Connect Flow**:
```kotlin
data class AuthSession(
    val walletAddress: WalletAddress,
    val sessionToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val firebaseToken: String
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticating(val walletAddress: WalletAddress? = null) : AuthState()
    data class Authenticated(val session: AuthSession) : AuthState()
    data class Error(val error: AppError) : AuthState()
}
```

**Transaction Build vs Broadcast**:
```kotlin
data class TransactionPayload(
    val from: WalletAddress,
    val to: WalletAddress,
    val value: BigDecimal,
    val data: String,
    val gasLimit: BigDecimal,
    val gasPrice: BigDecimal,
    val estimatedGasUSD: BigDecimal,
    val nonce: Long
)

// Build (estimate only)
suspend fun buildTransaction(to: WalletAddress, amount: BigDecimal): Resource<TransactionPayload>

// Broadcast (send)
suspend fun broadcastTransaction(signedTx: String, chain: ChainId): Resource<TxHash>
```

## References
- Execution steps: `resources/execution-protocol.md`
- Code examples: `resources/examples.md`
- Checklist: `resources/checklist.md`
- Error recovery: `resources/error-playbook.md`
- Patterns: `resources/patterns.md`
- Lessons learned: `.agent/.shared/lessons-learned.md`
