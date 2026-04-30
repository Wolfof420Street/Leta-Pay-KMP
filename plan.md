# Plan: Leta Pay — KMP Chat-Based Crypto Wallet (v5 — Audited & Corrected)

> **Changelog from v4**: Corrected Koog version and API surface; resolved Firebase token lifecycle gap; added KMP SSE client implementation notes; added per-wallet AI rate limiting; clarified SQLDelight vs Exposed on backend; tightened Coinbase CDP integration patterns; added ENS resolution flow; hardened refresh token spec (Argon2id); fixed phase delivery model; added error budget table; added missing test coverage requirements; corrected idempotency edge cases; added OWASP Web3-specific threat model notes.

---

## LOCKED DECISIONS

| Decision | Choice |
|----------|--------|
| Custody model | **Non-custodial** — WalletConnect; user signs, app never touches private keys |
| Chains (MVP) | **Ethereum (1), Polygon (137), Base (8453)** |
| Authentication | **WalletConnect** — external wallet owns key security |
| Transaction approval | **Tap-to-confirm only** (biometric gating deferred to post-MVP) |
| Gas policy | **Auto-estimate + visible USD cost + optional manual override** |
| Notifications | **Push required in MVP** — FCM plumbing Phase 1, notification flows Phase 7 |
| Chat scope | **1:1 direct messages only** (group chats post-MVP) |
| MVP features | **All 6**: wallet connect, chat, send payment, history, swaps, staking |
| Interaction model | **AI-driven agentic NLP first** (text/voice → intent → orchestrated action), manual UI fallback |
| AI framework | **Koog (JetBrains) 0.x** with Ktor plugin — multi-provider (OpenAI MVP), structured output, streaming via Flow |
| Local persistence | **SQLDelight** (KMP client) + **Exposed + HikariCP** (Ktor backend) |
| Real-time chat | **Firebase Realtime DB** — custom auth token (wallet-backed, NOT anonymous) |
| Staking MVP scope | **One provider per chain max**: Lido on Ethereum, AAVE on Polygon |
| Web secure storage | **sessionToken in-memory only; NO persistent refreshToken on web** |
| Compliance | **Address screening before every value transfer** (Coinbase Risk Assessment API) |
| Idempotency | **Client-generated UUID v4 per value-moving action; server enforces one-time use with 24 h TTL** |
| Price feed | **Coinbase Advanced Trade API** (`/api/v3/brokerage/products`) — 30–60 s cache |
| Transaction history | **Coinbase CDP Onchain Data API** (Etherscan/Polygonscan/Basescan as chain-specific fallbacks) |
| Swap routing | **Coinbase DEX Aggregator via CDP** (0x protocol as fallback) |
| Kill switch | **Redis/DB boolean flag** checked server-side on every value-moving endpoint |
| Backend runtime | **Ktor 3.x (JVM 21)** — Docker container (Railway/Fly.io for MVP; AWS/GCP migration path) |
| Delivery model | **Phased — day estimates are overlap targets, not calendar commitments** |
| Refresh token hashing | **Argon2id** (PHC-recommended; memory: 64 MB, iterations: 3, parallelism: 4) |
| ENS resolution | **Feature-flagged; backend resolves via Ethereum RPC `eth_call` to ENS registry** |

---

## AUDIT FINDINGS & RESOLUTIONS

### CRITICAL: Koog version and API surface

**v4 issue**: Dependencies listed `ai.koog:koog-core:1.0.0` and called non-existent methods `generateObject<T>()` and `executeStreaming()`. Koog is pre-1.0 (current stable: `0.x`). Its actual API uses agents with `AIAgent`, tool definitions, and `flow`-based streaming.

**Resolution**: All dependency versions pinned to `0.2.0` (latest at time of writing — pin and verify at implementation time via [JetBrains Space](https://packages.jetbrains.team/maven/p/koog/maven)). API patterns corrected in the implementation section below. Deterministic parser remains the primary fast path; Koog is used for ambiguous/complex intents only.

### HIGH: Firebase custom token expiry gap

**v4 issue**: Firebase custom tokens expire in 1 hour. The plan didn't specify what happens when the Firebase token expires while the app session is still valid (session = 30 min, refresh = 7 days). If the client loses Firebase auth mid-session, chat breaks silently.

**Resolution**: Backend mints a fresh Firebase custom token on every `/auth/refresh-token` call. Client monitors `FirebaseAuth.authStateChanges()` and re-authenticates Firebase whenever the ID token refresh fails, using the existing app session to hit `/auth/firebase-token` for a fresh custom token without requiring the user to re-sign.

### HIGH: KMP SSE client not specified

**v4 issue**: Plan stated "SSE streaming works via KMP Flow" but didn't address that `ktor-client` SSE support (`ktor-client-cio`) has platform-specific quirks on iOS and Web.

**Resolution**: Added explicit SSE client implementation per platform in the network section. iOS uses `URLSession`-backed engine with chunked response streaming. Web uses a JS `EventSource` wrapper bridged via `expect/actual`. Android and Desktop use CIO directly.

### HIGH: No rate limiting on AI endpoints

**v4 issue**: `/ai/parse` and `/ai/plan` invoke LLM APIs. A single wallet could exhaust the OpenAI quota in seconds with no per-user limit.

**Resolution**: Added per-wallet rate limit to all AI endpoints: 60 parse requests / minute, 20 plan requests / minute. Implemented via in-memory sliding window in Ktor (Redis for multi-instance deployment). 429 responses include `Retry-After` header. Rate limit state tracked by `walletAddress` extracted from JWT claims.

### MEDIUM: SQLDelight on backend ambiguous

**v4 issue**: Plan said "SQLDelight (KMP-native)" for local persistence but didn't clarify the backend database. SQLDelight generates type-safe Kotlin from SQL but is optimized for client use; using it on a JVM backend server with connection pooling requires extra configuration.

**Resolution**: Client uses SQLDelight. Backend uses **Exposed ORM + HikariCP** (battle-tested on JVM, supports connection pooling, works well with Ktor). Shared domain models (`ParseResult`, `ExecutionPlan`, etc.) are `@Serializable` Kotlin data classes in the `shared/core/model` module — backend maps to/from Exposed entities, client maps to/from SQLDelight queries.

### MEDIUM: Coinbase CDP integration underspecified

**v4 issue**: "Coinbase CDP Onchain Data API" is a platform, not a single endpoint. The integration pattern was vague.

**Resolution**: Specific API endpoints documented per use case in the Coinbase integration section below.

### MEDIUM: Argon2 variant not specified

**v4 issue**: "Argon2 for refresh token storage" — Argon2 has three variants (Argon2i, Argon2d, Argon2id). Argon2id is the OWASP-recommended default.

**Resolution**: Explicitly use **Argon2id** with parameters: memory=65536 KB, iterations=3, parallelism=4. Store as PHC string format. Pepper stored in `REFRESH_TOKEN_PEPPER` env variable (separate from the hash).

### LOW: Phase day numbers misleading

**v4 issue**: Phases had day ranges (e.g., "Days 1–2") without clarifying they overlap and are not calendar days.

**Resolution**: Phases are now described as relative sprint targets. A two-developer team working in parallel can complete phases simultaneously. Day estimates assume one developer on backend, one on shared/client.

### LOW: ENS resolution flow absent

**v4 issue**: `ENS_RESOLUTION_ENABLED` feature flag defined but no implementation described.

**Resolution**: ENS resolution flow added to Phase 4 (Transaction Intent). Backend resolves `.eth` names via `eth_call` to the ENS Public Resolver on mainnet. Resolved address cached for 10 minutes. Client sends raw ENS name; backend resolves and returns canonical address + ENS name in the build response.

---

## ARCHITECTURE DECISIONS

### Why Ktor + Koog over Vercel AI SDK

| Concern | Vercel AI SDK | Koog + Ktor |
|---------|--------------|-------------|
| Language | TypeScript — context switch from Kotlin | Kotlin — shared language with client |
| Streaming | `streamText().toDataStreamResponse()` | Koog agent `Flow<String>` → Ktor SSE |
| Structured output | Zod schemas | Kotlinx.serialization + Koog tool schemas |
| Multi-provider | `@ai-sdk/*` adapters | Single Koog config, swap provider |
| KMP shared code | None | Shared domain models, validation logic |
| Deployment | Vercel Edge (cold starts) | Any JVM host, containerized |
| MCP support | Manual | Built-in MCP protocol support (post-MVP) |

### Session & Auth Flow

```
1. App launch → check SecureStorage for valid sessionToken
2. Missing/expired → show AuthScreen
3. User taps "Connect Wallet" → WalletConnect deeplink
4. WalletConnect callback → wallet address received
5. POST /auth/request-nonce → nonce (UUID v4, 5 min TTL)
6. App requests SIWE (Sign-In with Ethereum) message signature
7. POST /auth/verify-signature → atomically consume nonce, verify signature
8. Backend issues: sessionToken (JWT, 30 min) + refreshToken (Argon2id-hashed, 7 days, rotating) + firebaseToken (custom token, 1 h)
9. Client stores tokens per platform (see storage matrix)
10. Client calls Firebase.signInWithCustomToken(firebaseToken)
11. Chat identity = wallet-backed app identity
```

**SIWE message format** (EIP-4361):
```
letapay.app wants you to sign in with your Ethereum account:
0xYOUR_ADDRESS

Sign in to Leta Pay

URI: https://letapay.app
Version: 1
Chain ID: 1
Nonce: <uuid_v4>
Issued At: <ISO8601>
Expiration Time: <ISO8601 + 5min>
```

### Refresh Token Rotation

- Refresh tokens are single-use; rotated on every `/auth/refresh-token` call
- Server stores: `Argon2id(token + pepper)` + device fingerprint + expiry + family ID
- Reuse detection (old token presented after rotation) → entire family revoked → forced full re-auth
- `POST /auth/revoke-session` available for explicit device logout

### Firebase Token Refresh Loop

```
Client monitors: FirebaseAuth.authStateChanges()
On ID token expiry failure:
  1. Check app sessionToken validity
  2. If valid: POST /auth/firebase-token → new Firebase custom token
  3. Firebase.signInWithCustomToken(newToken)
  4. Resume chat operations
On sessionToken expiry:
  1. POST /auth/refresh-token (with refreshToken)
  2. Receive new sessionToken + refreshToken + firebaseToken
  3. Update SecureStorage
  4. Re-authenticate Firebase
```

### Session Storage Per Platform

| Platform | sessionToken | refreshToken |
|----------|-------------|--------------|
| Android | EncryptedSharedPreferences | EncryptedSharedPreferences |
| iOS | Keychain (kSecAttrAccessibleWhenUnlockedThisDeviceOnly) | Keychain |
| Desktop | Encrypted local file (AES-256-GCM) | Encrypted local file |
| Web | **In-memory (JS heap) only** | **Not stored** — re-auth on tab close |

### State Management

`StateFlow<T>` + `Resource<T>` wrapper (Loading, Success, Error) in shared KMP code. No platform-specific state management libraries.

```kotlin
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val error: AppError) : Resource<Nothing>()
}
```

### Backend Architecture (Ktor)

```
backend-ktor/ (Kotlin/JVM 21, Ktor 3.x)
├── src/main/kotlin/
│   ├── Application.kt              # Module wiring
│   ├── ai/
│   │   ├── KoogConfig.kt           # Provider setup (OpenAI MVP)
│   │   ├── Agents.kt               # Koog AIAgent definitions
│   │   ├── Prompts.kt              # System prompt constants
│   │   ├── Schemas.kt              # @Serializable intent/plan models
│   │   └── DeterministicParser.kt  # Regex fast path
│   ├── routes/
│   │   ├── AuthRoutes.kt
│   │   ├── AiRoutes.kt             # /ai/parse, /ai/plan, /ai/chat-stream
│   │   ├── TransactionRoutes.kt
│   │   ├── SwapRoutes.kt
│   │   ├── YieldRoutes.kt
│   │   ├── PriceRoutes.kt
│   │   └── ContactRoutes.kt
│   ├── service/
│   │   ├── AuthService.kt
│   │   ├── FirebaseService.kt      # Custom token minting (Firebase Admin SDK)
│   │   ├── CoinbaseService.kt      # CDP REST client
│   │   ├── EnsService.kt           # ENS resolution via Ethereum RPC
│   │   ├── ScreeningService.kt     # Coinbase Risk Assessment
│   │   └── IdempotencyService.kt   # UUID v4 dedup with 24 h TTL
│   ├── middleware/
│   │   ├── AuthGuard.kt            # JWT Bearer verification
│   │   ├── RateLimiter.kt          # Per-wallet sliding window
│   │   ├── KillSwitch.kt           # DB/Redis flag circuit breaker
│   │   └── ErrorHandler.kt         # Consistent error envelope
│   ├── db/
│   │   ├── DatabaseFactory.kt      # HikariCP + Exposed setup
│   │   └── tables/                 # Exposed Table objects
│   └── model/                      # Domain models (mirrors shared/)
├── resources/
│   └── application.conf            # HOCON config
├── Dockerfile
└── build.gradle.kts
```

### Coinbase Integration Points (Specific Endpoints)

| Use case | Coinbase service | Specific endpoint |
|----------|-----------------|-------------------|
| Price feed / USD display | Advanced Trade API | `GET /api/v3/brokerage/products/{product_id}` |
| Transaction history | CDP Onchain Data | `GET /api/v2/accounts/{account_id}/transactions` or direct RPC via `eth_getTransactionsByAddress` (requires CDP API key) |
| Swap quote | CDP Swap API | `POST /api/v1/swap/quote` (0x-powered) |
| Swap execute | CDP Swap API | `POST /api/v1/swap/execute` (returns unsigned tx for wallet to sign) |
| Address screening | Risk Assessment API | `POST /api/v1/risk/address` |
| Gas estimation (Base) | Base RPC via CDP | `eth_estimateGas` + `eth_maxPriorityFeePerGas` via CDP RPC URL |
| Broadcast transaction | CDP | `eth_sendRawTransaction` via chain-specific RPC |

All Coinbase API keys are **server-side only** in Ktor backend. Never exposed to client.

### Error Budget

| Endpoint class | Target uptime | Max latency p99 | Circuit breaker threshold |
|----------------|--------------|-----------------|--------------------------|
| Auth routes | 99.9% | 500 ms | 5 failures in 30 s |
| AI parse | 99.5% | 3 000 ms | 3 failures in 60 s |
| AI plan | 99.0% | 8 000 ms | 3 failures in 60 s |
| Transaction build | 99.9% | 2 000 ms | 5 failures in 30 s |
| Price feed | 99.5% | 1 000 ms | — (cached fallback) |
| Swap quote | 99.0% | 5 000 ms | 3 failures in 60 s |

---

## KOOG AI FRAMEWORK — CORRECTED SETUP

> **Important**: Koog is at `0.x` as of this writing. The package coordinates and API shown below reflect the 0.2.x release train. Verify current version at https://packages.jetbrains.team/maven/p/koog/maven before implementation.

### Dependencies (backend-ktor/build.gradle.kts)

```kotlin
val koogVersion = "0.2.0" // VERIFY latest before use

dependencies {
    // Ktor core
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-sse:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    // Koog AI (JetBrains Space Maven — add repository)
    implementation("ai.koog:koog-agents:$koogVersion")
    implementation("ai.koog:koog-providers-openai:$koogVersion")
    // Uncomment to enable Anthropic fallback:
    // implementation("ai.koog:koog-providers-anthropic:$koogVersion")

    // Database (backend)
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    // Crypto / auth
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Ktor client (for Coinbase, Firebase Admin, ENS)
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    // Firebase Admin SDK (JVM)
    implementation("com.google.firebase:firebase-admin:9.3.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // DI
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")
}

// Add JetBrains Space repository for Koog
repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/koog/maven")
}
```

### Provider Configuration (ai/KoogConfig.kt)

```kotlin
import ai.koog.agents.core.provider.LLMProvider
import ai.koog.providers.openai.OpenAILLMProvider
import io.ktor.server.application.*

// Koog 0.2.x uses LLMProvider, not a Ktor plugin install
object KoogConfig {
    fun buildOpenAIProvider(apiKey: String): LLMProvider =
        OpenAILLMProvider(apiKey = apiKey)
}

// Model ID constants — use string IDs until Koog stabilises its model enum
object AiModels {
    const val PARSE = "gpt-4o-mini"   // Fast, cheap, good at structured extraction
    const val PLAN  = "gpt-4o"        // Strong reasoning for multi-step planning
    const val CHAT  = "gpt-4o-mini"   // Streaming summaries
}

// Provider swap: to switch to Anthropic, replace OpenAILLMProvider with
// AnthropicLLMProvider(apiKey = ...) and update model IDs.
// All agent code is provider-agnostic.
```

---

## KOOG IMPLEMENTATION — CORRECTED PATTERNS

> The 0.2.x API uses `AIAgent` + tool definitions for structured tasks. `generateObject<T>()` does not exist in 0.2.x — structured output is achieved by defining a tool the agent must call, then extracting the tool call arguments from the response. This is the standard pattern for structured LLM output with any framework.

### Pattern 1: Structured Intent Parsing via Tool-Calling

```kotlin
// ai/Agents.kt
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.*
import kotlinx.serialization.json.*

// Define a tool whose schema IS the ParseResult shape.
// Koog will force the LLM to call this tool, giving us typed output.
val parseResultTool = Tool(
    name = "return_parse_result",
    description = "Return the structured parse result for the user command",
    parameters = ParseResult.serializer().descriptor,
    execute = { args -> args } // Echo — we only need the structured args
)

class ParseAgent(private val provider: LLMProvider) {

    suspend fun parse(message: String, context: ParseContext): ParseResult {
        // Fast path: deterministic regex
        deterministicParse(message)?.let { return it }

        // Koog agent with forced tool call
        val agent = AIAgent(
            provider = provider,
            model = AiModels.PARSE,
            systemPrompt = PARSE_SYSTEM_PROMPT,
            tools = listOf(parseResultTool),
            toolChoice = ToolChoice.Specific("return_parse_result"), // force structured output
            temperature = 0.1
        )

        val response = agent.run(buildParsePrompt(message, context))

        // Extract typed result from tool call arguments
        return response.toolCalls
            .firstOrNull { it.name == "return_parse_result" }
            ?.let { Json.decodeFromJsonElement<ParseResult>(it.arguments) }
            ?: ParseResult(
                intent = IntentType.Unknown,
                confidence = 0.0,
                entities = Entities(),
                missingRequired = listOf("Could not parse intent"),
                safetyFlags = emptyList(),
                normalizedCommand = message,
                parserVersion = "koog-fallback-v1"
            )
    }
}
```

### Pattern 2: Execution Planning via Tool-Calling

```kotlin
class PlanAgent(private val provider: LLMProvider) {

    val planResultTool = Tool(
        name = "return_execution_plan",
        description = "Return the structured execution plan",
        parameters = ExecutionPlan.serializer().descriptor,
        execute = { args -> args }
    )

    suspend fun plan(
        parseResult: ParseResult,
        walletAddress: String,
        dryRun: Boolean
    ): ExecutionPlan {

        val agent = AIAgent(
            provider = provider,
            model = AiModels.PLAN,
            systemPrompt = PLAN_SYSTEM_PROMPT,
            tools = listOf(planResultTool),
            toolChoice = ToolChoice.Specific("return_execution_plan"),
            temperature = 0.2
        )

        val response = agent.run(
            Json.encodeToString(PlanInput(parseResult, walletAddress, dryRun))
        )

        return response.toolCalls
            .firstOrNull { it.name == "return_execution_plan" }
            ?.let { Json.decodeFromJsonElement<ExecutionPlan>(it.arguments) }
            ?: throw PlanGenerationError("Agent returned no plan tool call")
    }
}
```

### Pattern 3: Streaming Chat Summary via Koog + Ktor SSE

```kotlin
// ai/Agents.kt
class ChatSummaryAgent(private val provider: LLMProvider) {

    // Returns a Flow<String> of token chunks
    fun streamSummary(event: String, txResult: TxResult?): Flow<String> {
        val agent = AIAgent(
            provider = provider,
            model = AiModels.CHAT,
            systemPrompt = CHAT_SUMMARY_SYSTEM_PROMPT,
            maxTokens = 120,
            temperature = 0.7
        )
        return agent.streamRun(buildSummaryPrompt(event, txResult))
    }
}

// routes/AiRoutes.kt
fun Route.aiRoutes(
    parseAgent: ParseAgent,
    planAgent: PlanAgent,
    chatAgent: ChatSummaryAgent
) {
    authenticate("session-auth") {

        post("/ai/parse") {
            val principal = call.principal<WalletPrincipal>()!!
            checkRateLimit(principal.walletAddress, RateLimitBucket.AI_PARSE)

            val request = call.receive<ParseRequest>()
            val result = parseAgent.parse(request.message, ParseContext(principal.walletAddress))
            call.respond(result)
        }

        post("/ai/plan") {
            val principal = call.principal<WalletPrincipal>()!!
            checkRateLimit(principal.walletAddress, RateLimitBucket.AI_PLAN)

            val request = call.receive<PlanRequest>()
            val plan = planAgent.plan(request.parseResult, principal.walletAddress, request.dryRun)
            call.respond(plan)
        }

        // SSE: GET /ai/chat-stream?event=TransactionConfirmed&txHash=0x...
        sse("/ai/chat-stream") {
            val principal = call.principal<WalletPrincipal>()!!
            val event = call.parameters["event"] ?: return@sse close()
            val txResult = call.parameters["txHash"]?.let { fetchTx(it) }

            chatAgent.streamSummary(event, txResult).collect { chunk ->
                send(ServerSentEvent(data = chunk))
            }
        }
    }
}
```

### Pattern 4: Deterministic Fallback Parser (unchanged — primary fast path)

```kotlin
// ai/DeterministicParser.kt
private val SEND_ETH = Regex(
    """send\s+([\d.]+)\s+(\w+)\s+to\s+(0x[a-fA-F0-9]{40}|[\w.-]+\.eth)""",
    RegexOption.IGNORE_CASE
)
private val SWAP = Regex(
    """swap\s+([\d.]+)\s+(\w+)\s+(?:to|for)\s+(\w+)(?:\s+on\s+(\w+))?""",
    RegexOption.IGNORE_CASE
)
private val BALANCE_WORDS = setOf("balance", "how much", "what's in", "check wallet")
private val HISTORY_WORDS = setOf("history", "transactions", "activity", "recent", "past")

fun deterministicParse(message: String): ParseResult? {
    SEND_ETH.find(message)?.let { match ->
        return ParseResult(
            intent = IntentType.SendPayment,
            confidence = 0.95,
            entities = Entities(
                amount = match.groupValues[1],
                asset = match.groupValues[2].uppercase(),
                recipient = match.groupValues[3]
            ),
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1"
        )
    }

    SWAP.find(message)?.let { match ->
        return ParseResult(
            intent = IntentType.SwapAsset,
            confidence = 0.92,
            entities = Entities(
                amount = match.groupValues[1],
                fromAsset = match.groupValues[2].uppercase(),
                toAsset = match.groupValues[3].uppercase(),
                chain = match.groupValues.getOrNull(4)?.let { resolveChainId(it) }
            ),
            missingRequired = emptyList(),
            safetyFlags = emptyList(),
            normalizedCommand = message.trim(),
            parserVersion = "deterministic-v1"
        )
    }

    val lower = message.lowercase()
    if (BALANCE_WORDS.any { lower.contains(it) }) return ParseResult(
        intent = IntentType.CheckBalance, confidence = 0.90,
        entities = Entities(), missingRequired = emptyList(),
        safetyFlags = emptyList(), normalizedCommand = message.trim(),
        parserVersion = "deterministic-v1"
    )

    if (HISTORY_WORDS.any { lower.contains(it) }) return ParseResult(
        intent = IntentType.ShowHistory, confidence = 0.88,
        entities = Entities(), missingRequired = emptyList(),
        safetyFlags = emptyList(), normalizedCommand = message.trim(),
        parserVersion = "deterministic-v1"
    )

    return null // Falls through to Koog AI parser
}

private fun resolveChainId(name: String): Long? = when (name.lowercase()) {
    "eth", "ethereum", "mainnet" -> 1L
    "poly", "polygon", "matic"   -> 137L
    "base"                       -> 8453L
    else                         -> null
}
```

---

## FORMAL INTENT SCHEMA (Kotlinx Serialization)

```kotlin
// shared/core/model/Intent.kt — used by both client and backend

@Serializable
data class ParseResult(
    val intent: IntentType,
    val confidence: Double,             // 0.0–1.0
    val entities: Entities,
    val missingRequired: List<String>,  // fields needed before planning
    val safetyFlags: List<String>,      // detected risk signals
    val normalizedCommand: String,
    val parserVersion: String           // "deterministic-v1" | "koog-<provider>-v1"
)

@Serializable
enum class IntentType {
    SendPayment, SwapAsset, StakeAsset, CheckBalance, ShowHistory, Unknown
}

@Serializable
data class Entities(
    val recipient: String? = null,      // 0x address or ENS name
    val amount: String? = null,         // decimal string, not float (precision)
    val asset: String? = null,          // e.g. "ETH", "USDC"
    val fromAsset: String? = null,
    val toAsset: String? = null,
    val chain: Long? = null,            // EIP-155 chain ID
    val slippageBps: Int? = null,       // basis points, e.g. 50 = 0.5%
    val opportunityId: String? = null,
    val timeRange: String? = null,
    val status: String? = null
)

@Serializable
data class ExecutionPlan(
    val planId: String,                 // UUID v4
    val planType: PlanType,
    val requiresClarification: Boolean,
    val clarificationQuestions: List<ClarificationQuestion>?,
    val preview: ExecutionPreview?,     // human-readable cost breakdown
    val steps: List<ExecutionStep>,
    val policy: PolicyDecision,
    val idempotencyKey: String?,        // client must echo back on execute
    val expiresAt: Long                 // epoch ms; plan is invalid after this
)

@Serializable
data class ExecutionStep(
    val stepId: String,
    val kind: StepKind,                 // BuildTx, SignTx, BroadcastTx, Swap, Stake
    val endpoint: String,               // backend route to call
    val requiresUserConfirmation: Boolean,
    val timeoutMs: Long
)

@Serializable
data class ExecutionPreview(
    val description: String,
    val estimatedFeeUsd: String,        // decimal string
    val estimatedReceivedAmount: String?,
    val priceImpactBps: Int?,
    val networkName: String
)

// Validation rules (enforced in PolicyGuard before plan generation):
// - chain in {1, 137, 8453}
// - asset in MVP whitelist: ETH, MATIC, ETH_BASE, USDC, USDT, DAI
// - amount > 0 and within per-chain decimal precision limits
// - recipient is a valid 0x address OR a valid ENS name (resolved before plan)
// - slippageBps in [10, 1000] if specified (0.1%–10%)
```

---

## KMP SSE CLIENT — PLATFORM IMPLEMENTATION

SSE streaming from Ktor to KMP clients requires platform-specific handling.

```kotlin
// shared/core/network/SseClient.kt

expect fun createSseFlow(url: String, token: String): Flow<String>

// Android & Desktop (CIO engine supports SSE natively via HttpStatement):
// actual fun createSseFlow(url, token) = flow {
//     httpClient.prepareGet(url) { header("Authorization", "Bearer $token") }
//         .execute { response ->
//             response.bodyAsChannel().let { channel ->
//                 while (!channel.isClosedForRead) {
//                     val line = channel.readUTF8Line() ?: break
//                     if (line.startsWith("data:")) emit(line.removePrefix("data:").trim())
//                 }
//             }
//         }
// }

// iOS (Darwin engine — use URLSession chunked response):
// actual fun createSseFlow(url, token) — bridges to NSURLSession dataTask
// with streaming delegate; each chunk parsed for SSE data: lines.

// Web (JS) — bridge to native EventSource API:
// actual fun createSseFlow(url, token) = callbackFlow {
//     val es = js("new EventSource(url)")
//     es.onmessage = { e: dynamic -> trySend(e.data as String) }
//     es.onerror = { close() }
//     awaitClose { es.close() }
// }
```

---

## MODULE STRUCTURE

```
shared/ (KMP)
├── core/
│   ├── model/          ParseResult.kt, ExecutionPlan.kt, etc.
│   ├── error/          AppError sealed type
│   ├── resource/       Resource<T> wrapper
│   ├── network/        Ktor client setup, SseClient (expect/actual)
│   ├── database/       SQLDelight schema + queries (client persistence)
│   ├── storage/        SecureStorage (expect/actual)
│   └── di/             Koin module
├── feature/
│   ├── auth/           WalletConnectService, AuthRepository
│   ├── agent/          AgentOrchestrator, KmpAiClient
│   ├── wallet/         BalanceRepository, WalletViewModel
│   ├── chat/           Firebase Realtime, local cache, offline queue
│   ├── trade/          SwapRepository, SwapViewModel
│   └── yield/          StakingRepository, StakingViewModel
└── ui/                 Compose Multiplatform screens

backend-ktor/ (JVM 21)
├── src/main/kotlin/
│   ├── Application.kt
│   ├── ai/             ParseAgent, PlanAgent, ChatSummaryAgent, DeterministicParser
│   ├── routes/
│   ├── service/
│   ├── middleware/
│   ├── db/             Exposed tables + DatabaseFactory
│   └── model/
├── resources/application.conf
├── Dockerfile
└── build.gradle.kts
```

---

## AGENTIC NLP WORKFLOW

```
1. User types free-form text in chat input

2. POST /ai/parse
   → DeterministicParser (regex) — ~0 ms, handles ~60% of MVP intents
   → On miss: Koog ParseAgent (OpenAI GPT-4o-mini, tool-call mode) — ~400 ms
   → Returns: ParseResult (typed intent + entities + confidence + safety flags)

3. PolicyGuard validates:
   - chain in MVP set
   - asset in whitelist
   - amount precision
   - recipient format
   - ENS resolution if needed (EnsService)
   - confidence >= threshold (0.75 for value-moving intents)
   → Below threshold: return clarification prompt to user

4. POST /ai/plan
   → Koog PlanAgent (GPT-4o, tool-call mode) — ~1 500 ms
   → ScreeningService.check(recipient) — Coinbase Risk Assessment
   → Returns: ExecutionPlan (steps + preview + idempotency key + expiry)

5. Client presents ExecutionPreview:
   - Estimated fee in USD
   - Estimated received amount (swaps)
   - Network name
   - "Confirm" / "Cancel" buttons

6. User taps Confirm → AgentOrchestrator executes steps:
   - POST /transactions/build → unsigned tx + gas estimate
   - WalletConnect signs unsigned tx (external wallet, user approves)
   - POST /transactions/send → broadcast via CDP RPC
   - Poll /transactions/status/{txHash}

7. On confirmation event:
   - GET /ai/chat-stream?event=TransactionConfirmed&txHash=0x...
   - Koog ChatSummaryAgent streams natural-language summary via SSE
   - Client appends streaming tokens to chat message bubble
```

### Agent Safety Rules

- NEVER auto-sign or bypass wallet; external wallet always has final approval
- NEVER execute ambiguous intents without clarification (confidence < 0.75)
- ALWAYS present ExecutionPreview (fees + slippage + received amount) before confirm
- ALWAYS screen recipient address before building any value-moving transaction
- ALWAYS attach idempotency key to value-moving calls; reject duplicates within 24 h
- Record parser provider, model version, prompt version, and latency in telemetry

### Confidence Thresholds

| Intent type | Minimum confidence to auto-plan | Below threshold action |
|-------------|--------------------------------|----------------------|
| CheckBalance, ShowHistory | 0.60 | Clarify |
| SendPayment, SwapAsset, StakeAsset | 0.75 | Clarify |
| Unknown | — | Always clarify |

> These are initial defaults. Calibrate against labeled eval set before beta. Maintain confusion matrix per release.

---

## PHASE 1: Setup, DI, Core Network, Root Shell

**Sprint target**: 2 developer-days (1 backend, 1 client — can overlap)

### Backend: Ktor Module Bootstrap

```kotlin
// Application.kt
fun Application.module() {
    configureDI()
    configureDatabaseFactory()   // Exposed + HikariCP
    configureKoog()              // LLM provider init
    configureSecurity()          // JWT + CORS
    configureSerialization()     // kotlinx-json
    configureMonitoring()        // Micrometer / structured logging
    configureRateLimiting()
    configureErrorHandling()
    configureRouting()           // install all routes
}
```

### Environment Config (HOCON)

```hocon
ktor {
    deployment { port = 8080, port = ${?PORT} }
    application { modules = [ com.letapay.ApplicationKt.module ] }
}
openai { apiKey = ${OPENAI_API_KEY} }
firebase { serviceAccountPath = ${?FIREBASE_SA_PATH}, serviceAccountJson = ${?FIREBASE_SA_JSON} }
coinbase { apiKey = ${COINBASE_API_KEY}, riskApiKey = ${COINBASE_RISK_KEY} }
database { url = ${DATABASE_URL}, maxPoolSize = 10 }
security { sessionSecret = ${SESSION_SECRET}, refreshTokenPepper = ${REFRESH_TOKEN_PEPPER} }
rateLimit { aiParsePm = 60, aiPlanPm = 20 }
killSwitch { valueMoves = false, valueMoves = ${?KILL_SWITCH_VALUE_MOVES} }
```

### Database Schema (Exposed Tables)

```kotlin
object Sessions : Table("sessions") {
    val id = varchar("id", 36)           // UUID
    val walletAddress = varchar("wallet_address", 42)
    val refreshTokenHash = text("refresh_token_hash")  // Argon2id PHC string
    val familyId = varchar("family_id", 36)
    val deviceFingerprint = text("device_fingerprint").nullable()
    val expiresAt = long("expires_at")
    val revokedAt = long("revoked_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Nonces : Table("nonces") {
    val nonce = varchar("nonce", 36)
    val walletAddress = varchar("wallet_address", 42)
    val expiresAt = long("expires_at")
    val usedAt = long("used_at").nullable()
    override val primaryKey = PrimaryKey(nonce)
}

object IdempotencyKeys : Table("idempotency_keys") {
    val key = varchar("key", 36)
    val walletAddress = varchar("wallet_address", 42)
    val endpoint = varchar("endpoint", 200)
    val responseSnapshot = text("response_snapshot").nullable()
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(key)
}
```

### SQLDelight Schema (Client — unchanged from your implementation)

Tables: `chat_message`, `pending_message`, `transaction`, `token_metadata`, `staking_position`, `device_token`, `contact`

### Feature Flags

```kotlin
data class FeatureFlags(
    val YIELD_ENABLED: Boolean = false,
    val SWAP_ENABLED: Boolean = false,
    val VOICE_INPUT_ENABLED: Boolean = false,
    val BASE_STAKING_ENABLED: Boolean = false,
    val ENS_RESOLUTION_ENABLED: Boolean = false,
    val USD_VALUE_ENABLED: Boolean = true,
    val KILL_SWITCH_VALUE_MOVES: Boolean = false
)
```

### Deliverable

All 4 platforms build. Ktor backend builds and starts locally. Koin graphs resolve on both sides. Navigation shell and feature flags work. `/ai/parse` endpoint returns a valid `ParseResult` for "send 0.1 ETH to 0x1234...". Spotless passes.

---

## PHASE 2: WalletConnect & Session Management

**Sprint target**: 2 developer-days (overlap with Phase 1)

### Auth Routes (AuthRoutes.kt)

```kotlin
fun Route.authRoutes(authService: AuthService) {

    post("/auth/request-nonce") {
        val body = call.receive<NonceRequest>()
        val nonce = authService.generateNonce(body.walletAddress)
        call.respond(NonceResponse(nonce = nonce, expiresAt = clock.now().plusMinutes(5).toEpochMs()))
    }

    post("/auth/verify-signature") {
        val body = call.receive<VerifyRequest>()
        // 1. Verify SIWE message format (EIP-4361)
        // 2. Recover signer address from (message, signature) using Web3j ecRecover
        // 3. Assert signer == body.walletAddress
        // 4. Atomically mark nonce as used (unique DB constraint prevents replay)
        // 5. Mint session + refresh + firebase tokens
        val result = authService.verify(body)
        call.respond(result)
    }

    post("/auth/refresh-token") {
        val body = call.receive<RefreshRequest>()
        val result = authService.rotateRefreshToken(body.refreshToken, call.deviceFingerprint())
        call.respond(result)
    }

    post("/auth/firebase-token") {
        // Returns a fresh Firebase custom token using existing session JWT
        authenticate("session-auth") {
            val principal = call.principal<WalletPrincipal>()!!
            val token = authService.mintFirebaseToken(principal.walletAddress, principal.sessionId)
            call.respond(mapOf("firebaseToken" to token))
        }
    }

    post("/auth/revoke-session") {
        authenticate("session-auth") {
            val principal = call.principal<WalletPrincipal>()!!
            authService.revokeSession(principal.sessionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

### Signature Verification (Web3j)

```kotlin
// service/AuthService.kt
// Add dependency: implementation("org.web3j:core:4.12.2")

fun verifySignature(walletAddress: String, message: String, signature: String): Boolean {
    val prefixedMessage = "\u0019Ethereum Signed Message:\n${message.length}$message"
    val msgHash = Hash.sha3(prefixedMessage.toByteArray(Charsets.UTF_8))
    val signatureData = Sign.SignatureData(
        signature.hexToBytes()[64],
        signature.hexToBytes().slice(0..31).toByteArray(),
        signature.hexToBytes().slice(32..63).toByteArray()
    )
    val recoveredKey = Sign.signedMessageHashToKey(msgHash, signatureData)
    val recoveredAddress = Keys.toChecksumAddress(Keys.getAddress(recoveredKey))
    return recoveredAddress.equals(walletAddress, ignoreCase = true)
}
```

### Deliverable

Connect wallet → session tokens stored → balances visible → session survives app restart. Auth endpoints tested with curl.

---

## PHASE 3: Chat Scaffolding & Firebase Custom Auth

**Sprint target**: 2 developer-days (overlap with Phase 2)

Firebase chat identity = wallet address. No anonymous users.

### Firebase Security Rules

```json
{
  "rules": {
    "chats": {
      "$chatId": {
        ".read": "auth != null && ($chatId.contains(auth.token.wallet_address))",
        ".write": "auth != null && ($chatId.contains(auth.token.wallet_address))",
        "messages": {
          "$messageId": {
            ".validate": "newData.hasChildren(['sender', 'content', 'timestamp']) && newData.child('sender').val() === auth.token.wallet_address"
          }
        }
      }
    }
  }
}
```

### Offline Message Queue

SQLDelight `pending_message` table with `retryCount` and `nextRetryAt` columns. `PendingMessageWorker` (coroutine-based, no WorkManager dependency in KMP shared) polls queue on network restoration. Max 3 retries with exponential backoff (1 s, 4 s, 16 s).

### Deliverable

Chat UI functional. Firebase authenticated. Offline queue persists and retries. Streaming SSE tokens append to message bubble in real time.

---

## PHASE 4: Transaction Intent & Send Payment

**Sprint target**: 3 developer-days

### Send Flow

```
User: "Send 25 USDC to vitalik.eth on Polygon"

1. DeterministicParser extracts: intent=SendPayment, amount=25, asset=USDC, recipient=vitalik.eth, chain=null
2. PolicyGuard: chain=null → clarify? No: default to Polygon (chain=137) if asset is USDC
3. EnsService.resolve("vitalik.eth") → canonical 0x address (cache 10 min)
4. ScreeningService.check(resolvedAddress) → pass / reject
5. POST /ai/plan → ExecutionPreview with USDC gas estimate in MATIC + USD
6. User confirms in UI
7. POST /transactions/build → unsigned ERC-20 transfer calldata + gas params
8. WalletConnect signs → client receives signed tx hex
9. POST /transactions/send {signedTx, idempotencyKey} → broadcast via CDP Polygon RPC
10. Poll /transactions/status/{txHash} until confirmed (or 5 min timeout)
11. GET /ai/chat-stream → stream "✓ Sent 25 USDC to vitalik.eth on Polygon..."
```

### ENS Resolution Service

```kotlin
// service/EnsService.kt (feature-flagged by ENS_RESOLUTION_ENABLED)

class EnsService(private val httpClient: HttpClient, private val cache: Cache<String, String>) {

    private val ENS_REGISTRY = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
    private val ETH_RPC = "https://eth-mainnet.g.alchemy.com/v2/${config.alchemyKey}"

    suspend fun resolve(ensName: String): String? {
        if (!ensName.endsWith(".eth")) return null
        cache.getIfPresent(ensName)?.let { return it }

        val namehash = computeNamehash(ensName)
        // eth_call to ENS Public Resolver addr(bytes32 node)
        val result = httpClient.post(ETH_RPC) {
            setBody(JsonRpcRequest("eth_call", listOf(
                mapOf("to" to ENS_REGISTRY, "data" to "0x3b3b57de$namehash"),
                "latest"
            )))
        }.body<JsonRpcResponse>()

        val address = result.result?.takeIf { it != "0x" + "0".repeat(64) }
            ?.let { "0x" + it.takeLast(40) }
        address?.let { cache.put(ensName, it) }
        return address
    }
}
```

### Transaction Routes

```kotlin
fun Route.transactionRoutes(txService: TransactionService, screening: ScreeningService) {
    authenticate("session-auth") {

        post("/transactions/build") {
            killSwitchGuard()  // 503 if kill switch active
            val request = call.receive<BuildRequest>()
            val principal = call.principal<WalletPrincipal>()!!

            if (!screening.check(request.to)) throw AddressRejectedError(request.to)

            val result = txService.buildTransaction(request, principal.walletAddress)
            call.respond(result) // unsigned tx calldata + gas estimate + USD fee
        }

        post("/transactions/send") {
            killSwitchGuard()
            idempotencyGuard()
            val request = call.receive<SendRequest>()
            val txHash = txService.broadcastSigned(request)
            call.respond(SendResponse(txHash = txHash, status = "submitted"))
        }

        get("/transactions/status/{txHash}") {
            val status = txService.getStatus(call.parameters["txHash"]!!)
            call.respond(status)
        }

        get("/transactions/history") {
            val principal = call.principal<WalletPrincipal>()!!
            val history = txService.getHistory(principal.walletAddress, call.queryParameters)
            call.respond(history)
        }
    }
}
```

### Deliverable

Send via chat → ENS resolved → screened → WalletConnect signs → broadcast → confirmed → streaming summary. Idempotency prevents duplicate sends.

---

## PHASE 5: Trading / Swaps

**Sprint target**: 2 developer-days

### Quote Lifecycle

```
1. POST /swap/quote {fromAsset, toAsset, amount, chain, slippageBps}
   → Coinbase CDP Swap API POST /api/v1/swap/quote
   → Returns: quoteId, toAmount, rate, priceImpactBps, expiresAt (60 s TTL)

2. Client shows preview: "Receive ~X USDC | Price impact 0.12% | Fee $0.42"

3. User confirms → POST /swap/execute {quoteId, idempotencyKey}
   → CDP returns unsigned swap transaction calldata
   → WalletConnect signs → POST /transactions/send (reuses broadcast route)

4. On confirmation → streaming summary
```

### Quote Expiry Handling

Quotes expire in 60 seconds. Client shows countdown timer. On expiry, client re-fetches quote automatically (up to 3 times) and re-presents preview. After 3 re-fetches, user must manually retry.

### Deliverable

Swap via chat or manual UI. Quote expiry handled gracefully. Slippage protection enforced.

---

## PHASE 6: Staking / Yield

**Sprint target**: 2 developer-days

### MVP Staking Scope

| Chain | Provider | Protocol | Notes |
|-------|----------|----------|-------|
| Ethereum | Lido | stETH | `submit()` on Lido contract |
| Polygon | AAVE | aTokens | `deposit()` on AAVE LendingPool |

```
1. POST /yield/opportunities → list of active staking positions + current APY
2. POST /yield/stake {opportunityId, amount, idempotencyKey}
   → Build stake calldata → WalletConnect signs → broadcast
3. POST /yield/unstake {positionId, amount, idempotencyKey}
   → Lido: queue withdrawal (7-day unbonding) → AAVE: immediate
4. GET /yield/positions → current positions with live USD value
```

### Position Reconciliation

Background coroutine job in Ktor polls staking positions every 5 minutes and updates DB. Client polls `/yield/positions` every 60 seconds when yield screen is active.

### Deliverable

Stake/unstake via chat and manual UI. Positions show live. Unbonding state handled for Lido.

---

## PHASE 7: Push Notifications & Error Handling

**Sprint target**: 2 developer-days

### Push Architecture (Ktor + Firebase Admin SDK)

```kotlin
// service/ConfirmationWatcher.kt
class ConfirmationWatcher(private val db: Database, private val fcm: FirebaseMessaging) {

    // Launched as a Ktor coroutine background job on startup
    suspend fun watch() = coroutineScope {
        while (isActive) {
            val pending = db.getPendingWatchedTxs()
            pending.forEach { tx ->
                launch {
                    val status = pollCdpStatus(tx.txHash, tx.chain)
                    if (status.confirmed) {
                        db.markConfirmed(tx.txHash)
                        sendPushNotification(tx, status)
                    }
                }
            }
            delay(15_000)
        }
    }

    private fun sendPushNotification(tx: WatchedTx, status: TxStatus) {
        val message = Message.builder()
            .setToken(tx.deviceToken)
            .setNotification(Notification.builder()
                .setTitle("Transaction Confirmed")
                .setBody("${tx.amount} ${tx.asset} sent on ${tx.networkName}")
                .build())
            .putData("txHash", tx.txHash)
            .putData("type", "TX_CONFIRMED")
            .build()
        fcm.send(message)
    }
}
```

### Error Handling (Consistent Envelope)

```kotlin
// All errors return this shape:
@Serializable
data class ErrorResponse(
    val code: String,           // machine-readable e.g. "ADDRESS_REJECTED"
    val message: String,        // human-readable
    val retryAfter: Int? = null // seconds, for rate limit errors
)

// Error handler plugin:
install(StatusPages) {
    exception<AddressRejectedError> { call, e ->
        call.respond(HttpStatusCode.UnprocessableEntity,
            ErrorResponse("ADDRESS_REJECTED", e.message ?: "Address rejected by screening"))
    }
    exception<RateLimitExceededError> { call, e ->
        call.response.headers.append("Retry-After", e.retryAfterSeconds.toString())
        call.respond(HttpStatusCode.TooManyRequests,
            ErrorResponse("RATE_LIMIT_EXCEEDED", "Too many requests", e.retryAfterSeconds))
    }
    exception<KillSwitchActiveError> { call, _ ->
        call.respond(HttpStatusCode.ServiceUnavailable,
            ErrorResponse("KILL_SWITCH_ACTIVE", "Value-moving operations are temporarily suspended"))
    }
    // ... etc
}
```

### Deliverable

Push notifications delivered on tx confirmation. All endpoints return consistent error envelopes. Client surfaces human-readable errors in chat.

---

## PHASE 8: Polish, Testing, Performance

**Sprint target**: 2 developer-days

### Backend Performance

- Ktor: configure HikariCP pool size (default 10, tune per load test)
- Price feed: Redis cache layer with 30–60 s TTL; fallback to stale-while-revalidate
- AI response cache: cache identical deterministic parse results for 60 s (LRU in-memory)
- Circuit breakers per external service (Coinbase, OpenAI, Firebase)

### Required Test Coverage

| Layer | Test type | Minimum coverage |
|-------|-----------|-----------------|
| DeterministicParser | Unit | 100% of regex patterns |
| ParseAgent | Unit (mock LLM) | All 6 intent types |
| AuthService | Unit | Nonce generation, signature verify, token rotation, replay detection |
| TransactionRoutes | Integration (testApplication) | Build, send, kill switch, idempotency |
| SwapRoutes | Integration | Quote, execute, expiry |
| KmpAiClient | Integration | All 3 Ktor AI endpoints |
| E2E (Sepolia testnet) | Manual | Send ETH, swap USDC, check history |

### KMP Client Performance

- SQLDelight query plans reviewed for all list queries (chat messages, history)
- Lazy loading for transaction history (page size 20)
- Image/avatar caching (Coil on Android, custom on iOS/Desktop)

---

## PHASE 9: Deployment

**Sprint target**: 2 developer-days

### Ktor Docker Container

```dockerfile
# backend-ktor/Dockerfile

FROM gradle:8.10-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN gradle buildFatJar --no-daemon --no-configuration-cache

FROM eclipse-temurin:21-jre-alpine AS runtime
EXPOSE 8080
RUN addgroup -S letapay && adduser -S letapay -G letapay
USER letapay
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*-all.jar app.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

### Deployment Targets

| Platform | Deployment path |
|----------|----------------|
| Android | Play Store internal track → staged rollout |
| iOS | TestFlight → App Store |
| Web | Static hosting (Netlify/Vercel — no backend needed, calls Ktor) |
| Desktop | Platform installers (JVM bundled via jpackage) |
| Backend (Ktor) | **Railway or Fly.io for MVP** → AWS ECS or GCP Cloud Run for scale |

### Railway/Fly.io Setup

```yaml
# fly.toml (Fly.io)
app = "letapay-backend"
primary_region = "iad"
[build]
  dockerfile = "backend-ktor/Dockerfile"
[http_service]
  internal_port = 8080
  force_https = true
  auto_stop_machines = false
[env]
  JAVA_TOOL_OPTIONS = "-XX:+UseContainerSupport"
```

---

## ENVIRONMENT VARIABLES

| Variable | Used by | Notes |
|----------|---------|-------|
| `OPENAI_API_KEY` | Koog provider | Server-side only; never exposed to client |
| `AI_PARSE_MODEL` | ParseAgent | Default: `gpt-4o-mini` |
| `AI_PLAN_MODEL` | PlanAgent | Default: `gpt-4o` |
| `FIREBASE_SA_JSON` | Firebase Admin SDK | Service account JSON as env var (preferred over file path in containers) |
| `COINBASE_API_KEY` | Coinbase CDP | Server-side only |
| `COINBASE_RISK_KEY` | ScreeningService | Server-side only |
| `KILL_SWITCH_VALUE_MOVES` | KillSwitch middleware | Default: `false` |
| `SESSION_SECRET` | JWT signer (HS256) | Min 32 bytes random |
| `REFRESH_TOKEN_PEPPER` | Argon2id hasher | 32 bytes random; stored separately from hash |
| `DATABASE_URL` | HikariCP | PostgreSQL connection string |
| `ALCHEMY_API_KEY` | EnsService + RPC fallback | Server-side only |
| `REDIS_URL` | Rate limiter (multi-instance) | Optional; in-memory fallback for single instance |

---

## DEPENDENCY VERSIONS (VERSION CATALOG)

```toml
# gradle/libs.versions.toml

[versions]
kotlin = "2.1.0"
ktor = "3.0.1"
koog = "0.2.0"           # Verify at https://packages.jetbrains.team/maven/p/koog/maven
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.6.1"
koin = "3.5.6"
exposed = "0.55.0"
hikari = "5.1.0"
firebase-admin = "9.3.0"
web3j = "4.12.2"
argon2 = "2.11"
java-jwt = "4.4.0"
sqldelight = "2.0.2"

[libraries]
ktor-server-core       = { module = "io.ktor:ktor-server-core",                    version.ref = "ktor" }
ktor-server-netty      = { module = "io.ktor:ktor-server-netty",                   version.ref = "ktor" }
ktor-server-sse        = { module = "io.ktor:ktor-server-sse",                     version.ref = "ktor" }
ktor-server-auth       = { module = "io.ktor:ktor-server-auth",                    version.ref = "ktor" }
ktor-server-auth-jwt   = { module = "io.ktor:ktor-server-auth-jwt",                version.ref = "ktor" }
ktor-client-core       = { module = "io.ktor:ktor-client-core",                    version.ref = "ktor" }
ktor-client-cio        = { module = "io.ktor:ktor-client-cio",                     version.ref = "ktor" }
koog-agents            = { module = "ai.koog:koog-agents",                         version.ref = "koog" }
koog-openai            = { module = "ai.koog:koog-providers-openai",               version.ref = "koog" }
exposed-core           = { module = "org.jetbrains.exposed:exposed-core",          version.ref = "exposed" }
exposed-jdbc           = { module = "org.jetbrains.exposed:exposed-jdbc",          version.ref = "exposed" }
hikari                 = { module = "com.zaxxer:HikariCP",                          version.ref = "hikari" }
firebase-admin         = { module = "com.google.firebase:firebase-admin",          version.ref = "firebase-admin" }
web3j                  = { module = "org.web3j:core",                              version.ref = "web3j" }
argon2                 = { module = "de.mkammerer:argon2-jvm",                     version.ref = "argon2" }
java-jwt               = { module = "com.auth0:java-jwt",                          version.ref = "java-jwt" }
```

---

## SECURITY THREAT MODEL (Web3-Specific)

| Threat | Mitigation |
|--------|-----------|
| Signature replay | Nonces single-use; SIWE includes expiry timestamp |
| Address poisoning (look-alike addresses) | Show first 6 + last 4 chars always; full address on tap |
| Drainer contracts | Address screening via Coinbase Risk Assessment before every value move |
| Front-running (swap) | Slippage tolerance capped at 1000 bps; plan shows priceImpactBps |
| Refresh token theft | Argon2id + pepper; rotation; reuse detection revokes family |
| AI prompt injection via chat | Parse input sanitized; system prompt sandboxed; structured output schema rejects free-form injection |
| Kill switch bypass | Checked server-side on every value-moving route; client UI disables buttons but server is authoritative |
| Session fixation | New session ID on every auth; old tokens invalidated |
| Excessive API costs (AI) | Per-wallet rate limit (60 parse/min, 20 plan/min); deterministic parser handles most cases |
| Transaction history privacy | History queries gated by authenticated wallet address; no cross-wallet lookups |

---

## VERIFICATION CHECKLIST

- [ ] Ktor backend builds as Docker image (`docker build -t letapay-backend .`)
- [ ] All 4 client platforms build against Ktor backend (localhost:8080 in dev)
- [ ] Koin DI resolves on both backend and client with no missing bindings
- [ ] `/auth/request-nonce` + `/auth/verify-signature` flow works end-to-end
- [ ] Refresh token rotation works; reuse detection revokes family
- [ ] `/ai/parse` returns valid `ParseResult` for "Send 0.1 ETH to vitalik.eth"
- [ ] `/ai/chat-stream` streams via Ktor SSE; KMP SSE client appends tokens to chat bubble
- [ ] ENS resolution: "vitalik.eth" resolves to canonical address (feature-flagged)
- [ ] Address screening rejects known high-risk addresses (test with Coinbase sandbox)
- [ ] Kill switch returns 503 for all value-moving endpoints when flag active
- [ ] Idempotency keys block duplicate sends within 24 h
- [ ] Rate limiter returns 429 + `Retry-After` header when threshold exceeded
- [ ] Per-wallet rate limit: 60 parse/min and 20 plan/min enforced
- [ ] Full send flow on Sepolia testnet: chat → parse → plan → WalletConnect sign → broadcast → confirmation → streaming summary
- [ ] Firebase chat reconnects automatically after custom token expiry (1 h test)
- [ ] Spotless passes for all Kotlin source files
- [ ] Docker image runs in Fly.io/Railway with all env vars injected

---

## DEFERRED (POST-MVP)

- Group chats
- Voice input (VOICE_INPUT_ENABLED flag is ready)
- Biometric gating for transaction approval
- WebSocket transport replacing Firebase for chat
- Base chain staking
- MCP tool integrations (Koog has built-in MCP support — enable post-MVP)
- Hardware wallet support (Ledger via WalletConnect)
- Multi-account support
- Token allowlist expansion (beyond USDC/USDT/DAI)
- Fiat on-ramp integration
- Notification preference management UI
- Analytics / event tracking
- Mainnet deployment (MVP ships on testnets)