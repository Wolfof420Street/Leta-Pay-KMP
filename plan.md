# Plan: Leta Pay — KMP Chat-Based Crypto Wallet (v6 — AgentKit Edition)

> **Changelog from v5**: Integrated Coinbase AgentKit as a dedicated Node.js sidecar microservice (`agentkit-sidecar/`). AgentKit provides the canonical action layer for swaps, transfers, staking, and gas estimation — replacing the direct CDP REST calls that previously lived scattered across Ktor service classes. Ktor remains the auth, session, AI orchestration, and routing layer; it calls the sidecar over internal HTTP. This preserves the non-custodial model (user still signs via WalletConnect — AgentKit builds unsigned transactions, never holds user keys). Added AgentKit sidecar deployment config, updated Coinbase integration table, added AgentKit action catalog, updated Phase 5/6 delivery to use sidecar, added new env vars, updated threat model.

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
| Swap routing | **AgentKit sidecar** (uses CDP swap action provider internally) |
| Kill switch | **Redis/DB boolean flag** checked server-side on every value-moving endpoint |
| Backend runtime | **Ktor 3.x (JVM 21)** — Docker container (Railway/Fly.io for MVP; AWS/GCP migration path) |
| AgentKit runtime | **Node.js 20 sidecar** — separate container, internal network only, never exposed to public internet |
| Delivery model | **Phased — day estimates are overlap targets, not calendar commitments** |
| Refresh token hashing | **Argon2id** (PHC-recommended; memory: 64 MB, iterations: 3, parallelism: 4) |
| ENS resolution | **Feature-flagged; backend resolves via Ethereum RPC `eth_call` to ENS registry** |

---

## WHY AGENTKIT AS A SIDECAR

AgentKit is published as TypeScript (Node.js) and Python packages only. There is no JVM/Kotlin SDK. Rather than re-implementing its action primitives manually in Ktor, the cleanest integration is a thin **Node.js sidecar** that:

1. Wraps AgentKit's action providers (transfer, swap, stake, gas, contract calls) behind a simple internal REST API
2. Is called exclusively by the Ktor backend — never directly by clients
3. Runs on the internal Docker network only (`agentkit-sidecar:3100`, not exposed on any public port)
4. Holds the CDP API credentials; Ktor holds the OpenAI + Firebase + session credentials — clean separation

This means:
- The Ktor backend continues to own auth, AI orchestration (Koog), session management, rate limiting, kill switch, and idempotency
- The sidecar owns the "do the onchain thing" layer — building unsigned transactions, fetching quotes, checking balances, resolving gas
- The non-custodial guarantee is preserved: sidecar builds unsigned calldata, Ktor returns it to the client, WalletConnect signs it, client sends signed tx back to Ktor for broadcast

---

## ARCHITECTURE: WITH AGENTKIT SIDECAR

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PUBLIC INTERNET                              │
│                                                                      │
│   KMP Clients (Android / iOS / Web / Desktop)                       │
│        │                         │                                   │
│        │  HTTPS REST + SSE       │  WalletConnect deeplink           │
│        ▼                         ▼                                   │
│   ┌─────────────────────┐   ┌──────────────┐                        │
│   │   Ktor Backend      │   │  User Wallet  │                        │
│   │   (JVM 21, :8080)   │   │  (MetaMask    │                        │
│   │                     │   │   Rainbow     │                        │
│   │  - Auth & Sessions  │   │   etc.)       │                        │
│   │  - Koog AI agents   │   └──────────────┘                        │
│   │  - Rate limiting    │                                            │
│   │  - Kill switch      │                                            │
│   │  - Idempotency      │                                            │
│   └─────────┬───────────┘                                            │
│             │  Internal HTTP only (Docker network)                   │
└─────────────┼───────────────────────────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────────────────────────┐
│                       INTERNAL NETWORK                               │
│                                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │   AgentKit Sidecar (Node.js 20, :3100, NOT public-facing)  │   │
│   │                                                             │   │
│   │   POST /agentkit/transfer/build                             │   │
│   │   POST /agentkit/swap/quote                                 │   │
│   │   POST /agentkit/swap/build                                 │   │
│   │   POST /agentkit/stake/build                                │   │
│   │   POST /agentkit/gas/estimate                               │   │
│   │   POST /agentkit/address/screen                             │   │
│   │   GET  /agentkit/balance/:address/:network                  │   │
│   │                                                             │   │
│   │   WalletProvider: CdpEvmWalletProvider (READ-ONLY mode)     │   │
│   │   Action Providers: cdpApiActionProvider, swapActionProvider│   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐ │
│   │  PostgreSQL   │  │    Redis     │  │  Firebase Realtime DB    │ │
│   └──────────────┘  └──────────────┘  └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
              │
              ▼  Coinbase CDP APIs (external)
         ┌────────────────────────────────────┐
         │  Advanced Trade, Swap, Onchain Data │
         │  Risk Assessment, RPC endpoints     │
         └────────────────────────────────────┘
```

---

## AGENTKIT SIDECAR — FULL SPECIFICATION

### Directory Structure

```
agentkit-sidecar/ (Node.js 20, TypeScript)
├── src/
│   ├── index.ts               # Express app entry point
│   ├── agentkit.ts            # AgentKit + WalletProvider init
│   ├── routes/
│   │   ├── transfer.ts        # /agentkit/transfer/*
│   │   ├── swap.ts            # /agentkit/swap/*
│   │   ├── stake.ts           # /agentkit/stake/*
│   │   ├── gas.ts             # /agentkit/gas/*
│   │   ├── balance.ts         # /agentkit/balance/*
│   │   └── screen.ts          # /agentkit/address/screen
│   └── middleware/
│       ├── internalOnly.ts    # Reject requests not from Ktor (shared secret header)
│       └── errorHandler.ts
├── package.json
├── tsconfig.json
└── Dockerfile
```

### Dependencies (package.json)

```json
{
  "name": "letapay-agentkit-sidecar",
  "version": "1.0.0",
  "dependencies": {
    "@coinbase/agentkit": "^0.7.4",
    "express": "^4.19.0",
    "zod": "^3.22.0",
    "dotenv": "^16.4.0"
  },
  "devDependencies": {
    "typescript": "^5.4.0",
    "@types/express": "^4.17.21",
    "@types/node": "^20.0.0",
    "ts-node": "^10.9.0"
  }
}
```

### AgentKit Initialization (src/agentkit.ts)

```typescript
import { AgentKit, AgentKitConfig, CdpEvmWalletProvider, CdpEvmWalletProviderConfig,
         cdpApiActionProvider, swapActionProvider } from "@coinbase/agentkit";

// IMPORTANT: This wallet provider is used in READ-ONLY / BUILD mode only.
// It constructs and quotes unsigned transactions.
// It NEVER holds user private keys. User keys live in the user's external wallet.
// The "wallet address" passed here is the connected user's address (from Ktor JWT claims),
// injected per-request so the calldata is built for the right sender.

export async function buildAgentKit(userWalletAddress: string): Promise<AgentKit> {
  const walletProvider = new CdpEvmWalletProvider(
    new CdpEvmWalletProviderConfig({
      apiKeyId: process.env.CDP_API_KEY_ID!,
      apiKeySecret: process.env.CDP_API_KEY_SECRET!,
      walletSecret: process.env.CDP_WALLET_SECRET!,
      address: userWalletAddress,  // build calldata FROM this address
      networkId: "base-mainnet",   // overridden per-request via body
    })
  );

  return new AgentKit(new AgentKitConfig({
    walletProvider,
    actionProviders: [
      cdpApiActionProvider({
        apiKeyId: process.env.CDP_API_KEY_ID!,
        apiKeySecret: process.env.CDP_API_KEY_SECRET!,
      }),
      swapActionProvider(),
    ],
  }));
}
```

### Internal Security Middleware (src/middleware/internalOnly.ts)

```typescript
import { Request, Response, NextFunction } from "express";

// Sidecar is NEVER exposed publicly. Additionally, all requests must carry
// the shared SIDECAR_SECRET header — a second layer in case of network misconfiguration.
export function internalOnly(req: Request, res: Response, next: NextFunction) {
  const secret = req.headers["x-sidecar-secret"];
  if (secret !== process.env.SIDECAR_SECRET) {
    return res.status(403).json({ error: "Forbidden" });
  }
  next();
}
```

### Transfer Build Route (src/routes/transfer.ts)

```typescript
import { Router } from "express";
import { buildAgentKit } from "../agentkit";
import { z } from "zod";

const router = Router();

const TransferBuildSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  toAddress:   z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  asset:       z.enum(["ETH", "USDC", "USDT", "DAI", "MATIC"]),
  amount:      z.string(), // decimal string, e.g. "25.00"
  networkId:   z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
});

// POST /agentkit/transfer/build
// Returns: unsigned transaction calldata + gas estimate
// Ktor returns this to client → WalletConnect signs → client sends back for broadcast
router.post("/build", async (req, res, next) => {
  try {
    const body = TransferBuildSchema.parse(req.body);
    const agentKit = await buildAgentKit(body.fromAddress);

    // AgentKit builds the unsigned ERC-20 / native transfer calldata
    const actions = agentKit.getActions();
    const transferAction = actions.find(a => a.name === "transfer");
    if (!transferAction) throw new Error("Transfer action not available");

    const result = await transferAction.invoke({
      amount: body.amount,
      assetId: body.asset.toLowerCase(),
      destination: body.toAddress,
      gasless: false,  // user pays gas; we show estimate
    });

    res.json({ success: true, calldata: result });
  } catch (err) {
    next(err);
  }
});

export default router;
```

### Swap Routes (src/routes/swap.ts)

```typescript
import { Router } from "express";
import { buildAgentKit } from "../agentkit";
import { z } from "zod";

const router = Router();

const SwapQuoteSchema = z.object({
  fromAddress: z.string(),
  fromAsset:   z.string(),
  toAsset:     z.string(),
  amount:      z.string(),
  networkId:   z.string(),
  slippageBps: z.number().int().min(10).max(1000).optional().default(50),
});

// POST /agentkit/swap/quote
router.post("/quote", async (req, res, next) => {
  try {
    const body = SwapQuoteSchema.parse(req.body);
    const agentKit = await buildAgentKit(body.fromAddress);
    const actions = agentKit.getActions();
    const swapAction = actions.find(a => a.name === "swap");
    if (!swapAction) throw new Error("Swap action not available");

    const quote = await swapAction.invoke({
      fromAssetId: body.fromAsset.toLowerCase(),
      toAssetId:   body.toAsset.toLowerCase(),
      amount:      body.amount,
      quoteOnly:   true,     // dry-run; returns quote without executing
    });

    res.json({ success: true, quote });
  } catch (err) {
    next(err);
  }
});

// POST /agentkit/swap/build
// Returns unsigned swap calldata for WalletConnect to sign
router.post("/build", async (req, res, next) => {
  try {
    const body = SwapQuoteSchema.parse(req.body);
    const agentKit = await buildAgentKit(body.fromAddress);
    const actions = agentKit.getActions();
    const swapAction = actions.find(a => a.name === "swap");
    if (!swapAction) throw new Error("Swap action not available");

    const result = await swapAction.invoke({
      fromAssetId: body.fromAsset.toLowerCase(),
      toAssetId:   body.toAsset.toLowerCase(),
      amount:      body.amount,
      quoteOnly:   false,
    });

    res.json({ success: true, calldata: result });
  } catch (err) {
    next(err);
  }
});

export default router;
```

### Gas Estimation Route (src/routes/gas.ts)

```typescript
import { Router } from "express";
import { buildAgentKit } from "../agentkit";

const router = Router();

// POST /agentkit/gas/estimate
// Body: { fromAddress, toAddress, asset, amount, networkId }
// Returns: { estimatedGasUnits, gasPriceWei, estimatedFeeUsd, networkName }
router.post("/estimate", async (req, res, next) => {
  try {
    const { fromAddress, networkId, calldata } = req.body;
    const agentKit = await buildAgentKit(fromAddress);
    const actions = agentKit.getActions();
    const gasAction = actions.find(a => a.name === "get_balance"); // fallback to RPC

    // AgentKit exposes the wallet provider's RPC for gas estimation
    const provider = agentKit.walletProvider;
    const estimate = await provider.estimateGas({
      from: fromAddress,
      to: calldata?.to,
      data: calldata?.data,
    });

    res.json({ success: true, estimate });
  } catch (err) {
    next(err);
  }
});

export default router;
```

### Sidecar Entry Point (src/index.ts)

```typescript
import express from "express";
import { internalOnly } from "./middleware/internalOnly";
import transferRouter from "./routes/transfer";
import swapRouter from "./routes/swap";
import stakeRouter from "./routes/stake";
import gasRouter from "./routes/gas";
import balanceRouter from "./routes/balance";
import screenRouter from "./routes/screen";

const app = express();
app.use(express.json());
app.use(internalOnly); // All routes require internal secret

app.use("/agentkit/transfer", transferRouter);
app.use("/agentkit/swap", swapRouter);
app.use("/agentkit/stake", stakeRouter);
app.use("/agentkit/gas", gasRouter);
app.use("/agentkit/balance", balanceRouter);
app.use("/agentkit/address", screenRouter);

app.get("/health", (_, res) => res.json({ ok: true }));

const PORT = process.env.PORT || 3100;
app.listen(PORT, () => console.log(`AgentKit sidecar listening on :${PORT}`));
```

### Sidecar Dockerfile

```dockerfile
# agentkit-sidecar/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine AS runtime
WORKDIR /app
RUN addgroup -S letapay && adduser -S letapay -G letapay
USER letapay
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json .
EXPOSE 3100
CMD ["node", "dist/index.js"]
```

---

## KTOR SIDECAR CLIENT (service/AgentKitClient.kt)

The Ktor backend calls the sidecar via a dedicated internal HTTP client. All sidecar calls are wrapped in this single service class — swap it out or mock it easily.

```kotlin
// service/AgentKitClient.kt
class AgentKitClient(
    private val httpClient: HttpClient,
    private val sidecarUrl: String,       // e.g. "http://agentkit-sidecar:3100"
    private val sidecarSecret: String,    // SIDECAR_SECRET env var
) {
    private fun HttpRequestBuilder.withSidecarAuth() {
        header("x-sidecar-secret", sidecarSecret)
        contentType(ContentType.Application.Json)
    }

    // --- Transfer ---
    suspend fun buildTransfer(
        fromAddress: String, toAddress: String,
        asset: String, amount: String, networkId: String
    ): AgentKitCalldata = httpClient.post("$sidecarUrl/agentkit/transfer/build") {
        withSidecarAuth()
        setBody(buildJsonObject {
            put("fromAddress", fromAddress); put("toAddress", toAddress)
            put("asset", asset);             put("amount", amount)
            put("networkId", networkId)
        })
    }.body()

    // --- Swap ---
    suspend fun getSwapQuote(request: SwapQuoteRequest): AgentKitSwapQuote =
        httpClient.post("$sidecarUrl/agentkit/swap/quote") {
            withSidecarAuth(); setBody(request)
        }.body()

    suspend fun buildSwap(request: SwapQuoteRequest): AgentKitCalldata =
        httpClient.post("$sidecarUrl/agentkit/swap/build") {
            withSidecarAuth(); setBody(request)
        }.body()

    // --- Stake ---
    suspend fun buildStake(request: StakeRequest): AgentKitCalldata =
        httpClient.post("$sidecarUrl/agentkit/stake/build") {
            withSidecarAuth(); setBody(request)
        }.body()

    // --- Gas ---
    suspend fun estimateGas(fromAddress: String, calldata: JsonObject?): GasEstimate =
        httpClient.post("$sidecarUrl/agentkit/gas/estimate") {
            withSidecarAuth()
            setBody(buildJsonObject {
                put("fromAddress", fromAddress)
                if (calldata != null) put("calldata", calldata)
            })
        }.body()

    // --- Balance ---
    suspend fun getBalance(address: String, networkId: String): BalanceResponse =
        httpClient.get("$sidecarUrl/agentkit/balance/$address/$networkId") {
            withSidecarAuth()
        }.body()

    // Health check used in startup validation
    suspend fun isHealthy(): Boolean = runCatching {
        httpClient.get("$sidecarUrl/health").status.value == 200
    }.getOrDefault(false)
}

// Shared response models
@Serializable data class AgentKitCalldata(val success: Boolean, val calldata: JsonObject)
@Serializable data class AgentKitSwapQuote(val success: Boolean, val quote: JsonObject)
@Serializable data class GasEstimate(val estimatedGasUnits: Long, val gasPriceWei: String, val estimatedFeeUsd: String)
@Serializable data class BalanceResponse(val address: String, val balances: List<TokenBalance>)
@Serializable data class TokenBalance(val asset: String, val amount: String, val usdValue: String)
```

---

## UPDATED COINBASE INTEGRATION TABLE

| Use case | How it's done now (v6) | Service boundary |
|----------|----------------------|------------------|
| Transfer build (ETH/ERC-20) | AgentKit sidecar `/agentkit/transfer/build` | Sidecar → CDP |
| Swap quote | AgentKit sidecar `/agentkit/swap/quote` | Sidecar → CDP Swap |
| Swap build (unsigned tx) | AgentKit sidecar `/agentkit/swap/build` | Sidecar → CDP Swap |
| Stake build (Lido/AAVE calldata) | AgentKit sidecar `/agentkit/stake/build` | Sidecar → CDP |
| Gas estimation | AgentKit sidecar `/agentkit/gas/estimate` | Sidecar → chain RPC |
| Wallet balance | AgentKit sidecar `/agentkit/balance/:address/:network` | Sidecar → CDP Onchain |
| Transaction broadcast | Ktor `TransactionService.broadcastSigned()` directly via CDP RPC | Ktor → CDP RPC |
| Transaction history | Ktor `CoinbaseService` via CDP Onchain Data API | Ktor → CDP |
| Price feed / USD display | Ktor `CoinbaseService` via Advanced Trade API | Ktor → CDP |
| Address screening | Ktor `ScreeningService` via Risk Assessment API | Ktor → CDP |

**Note**: Transaction **broadcast** (`eth_sendRawTransaction`) stays in Ktor because it requires idempotency enforcement, kill switch, and audit logging — all of which live in Ktor middleware.

---

## AGENTKIT ACTION CATALOG (Available in MVP)

AgentKit ships these action providers used by the sidecar:

| Action provider | Actions used | MVP purpose |
|----------------|-------------|------------|
| `cdpApiActionProvider` | `transfer`, `get_balance` | Build transfer calldata; check wallet balance |
| `swapActionProvider` | `swap` (quote + build) | Swap quotes and unsigned calldata |
| Lido (via CDP contract calls) | `stake_eth` | Ethereum staking via Lido |
| AAVE (via CDP contract calls) | `deposit`, `withdraw` | Polygon yield via AAVE |
| Gas estimation | Built into WalletProvider | Gas + USD cost before confirm |

Post-MVP unlocks (available in AgentKit, feature-flagged off):
- `deployToken` — ERC-20 deployment
- `deployNFT` — NFT contract
- `morphoActionProvider` — additional yield
- `x402ActionProvider` — micropayment protocol
- `wowActionProvider` — token bonding curves

---

## UPDATED AGENTIC NLP WORKFLOW (with AgentKit)

```
1. User types free-form text in chat input

2. POST /ai/parse (Ktor)
   → DeterministicParser (regex) — ~0 ms, handles ~60% of MVP intents
   → On miss: Koog ParseAgent (GPT-4o-mini, tool-call mode) — ~400 ms
   → Returns: ParseResult (typed intent + entities + confidence + safety flags)

3. PolicyGuard (Ktor) validates:
   - chain in MVP set {1, 137, 8453}
   - asset in whitelist
   - amount precision
   - recipient format
   - ENS resolution if needed (EnsService)
   - confidence >= threshold (0.75 for value-moving intents)
   → Below threshold: return clarification prompt

4. POST /ai/plan (Ktor)
   → Koog PlanAgent (GPT-4o, tool-call mode) — ~1 500 ms
   → ScreeningService.check(recipient) — Coinbase Risk Assessment
   → [NEW] AgentKitClient.estimateGas() — real gas estimate from AgentKit sidecar
   → Returns: ExecutionPlan (steps + preview with REAL gas numbers + idempotency key + expiry)

5. Client presents ExecutionPreview (real numbers from AgentKit):
   - Estimated fee in USD (from AgentKit gas action)
   - Estimated received amount (from AgentKit swap quote)
   - Network name + price impact
   - "Confirm" / "Cancel" buttons

6. User taps Confirm → AgentOrchestrator executes:
   a. POST /transactions/build (Ktor)
      → Ktor calls AgentKitClient.buildTransfer() or .buildSwap() or .buildStake()
      → AgentKit sidecar returns unsigned calldata
      → Ktor returns unsigned tx to client
   b. WalletConnect signs unsigned tx (user approves in their wallet)
   c. POST /transactions/send (Ktor) — idempotency + kill switch enforced here
      → Ktor broadcasts via CDP RPC directly (NOT through sidecar — audit trail lives here)
   d. Poll /transactions/status/{txHash}

7. On confirmation:
   - GET /ai/chat-stream → Koog ChatSummaryAgent streams natural-language summary via SSE
   - Client appends streaming tokens to chat message bubble
```

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
│   ├── Application.kt
│   ├── ai/
│   │   ├── KoogConfig.kt
│   │   ├── Agents.kt
│   │   ├── Prompts.kt
│   │   ├── Schemas.kt
│   │   └── DeterministicParser.kt
│   ├── routes/
│   │   ├── AuthRoutes.kt
│   │   ├── AiRoutes.kt
│   │   ├── TransactionRoutes.kt     # calls AgentKitClient for build; handles broadcast directly
│   │   ├── SwapRoutes.kt            # calls AgentKitClient for quote + build
│   │   ├── YieldRoutes.kt           # calls AgentKitClient for stake build
│   │   ├── PriceRoutes.kt
│   │   └── ContactRoutes.kt
│   ├── service/
│   │   ├── AuthService.kt
│   │   ├── FirebaseService.kt
│   │   ├── AgentKitClient.kt        # [NEW] Internal HTTP client for sidecar
│   │   ├── CoinbaseService.kt       # Price feed, tx history, broadcast RPC
│   │   ├── EnsService.kt
│   │   ├── ScreeningService.kt
│   │   └── IdempotencyService.kt
│   ├── middleware/
│   │   ├── AuthGuard.kt
│   │   ├── RateLimiter.kt
│   │   ├── KillSwitch.kt
│   │   └── ErrorHandler.kt
│   ├── db/
│   │   ├── DatabaseFactory.kt
│   │   └── tables/
│   └── model/
├── resources/application.conf
├── Dockerfile
└── build.gradle.kts
```

### Error Budget

| Endpoint class | Target uptime | Max latency p99 | Circuit breaker threshold |
|----------------|--------------|-----------------|--------------------------|
| Auth routes | 99.9% | 500 ms | 5 failures in 30 s |
| AI parse | 99.5% | 3 000 ms | 3 failures in 60 s |
| AI plan | 99.0% | 8 000 ms | 3 failures in 60 s |
| Transaction build (via sidecar) | 99.5% | 3 000 ms | 3 failures in 30 s |
| Swap quote (via sidecar) | 99.0% | 5 000 ms | 3 failures in 60 s |
| Price feed | 99.5% | 1 000 ms | — (cached fallback) |
| AgentKit sidecar health | 99.9% | 200 ms | 5 failures in 30 s |

---

## KOOG AI FRAMEWORK — SETUP

> **Important**: Koog is at `0.x`. Verify current version at https://packages.jetbrains.team/maven/p/koog/maven before implementation.

### Dependencies (backend-ktor/build.gradle.kts)

```kotlin
val koogVersion = "0.2.0" // VERIFY latest before use

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-sse:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    // Koog AI (JetBrains Space Maven)
    implementation("ai.koog:koog-agents:$koogVersion")
    implementation("ai.koog:koog-providers-openai:$koogVersion")

    // Database (backend)
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    // Crypto / auth
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Ktor client (Coinbase, Firebase Admin, ENS, AgentKit sidecar)
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    // Firebase Admin SDK (JVM)
    implementation("com.google.firebase:firebase-admin:9.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/koog/maven")
}
```

### Provider Configuration (ai/KoogConfig.kt)

```kotlin
import ai.koog.agents.core.provider.LLMProvider
import ai.koog.providers.openai.OpenAILLMProvider

object KoogConfig {
    fun buildOpenAIProvider(apiKey: String): LLMProvider =
        OpenAILLMProvider(apiKey = apiKey)
}

object AiModels {
    const val PARSE = "gpt-4o-mini"
    const val PLAN  = "gpt-4o"
    const val CHAT  = "gpt-4o-mini"
}
```

---

## KOOG IMPLEMENTATION — PATTERNS

### Pattern 1: Structured Intent Parsing via Tool-Calling

```kotlin
val parseResultTool = Tool(
    name = "return_parse_result",
    description = "Return the structured parse result for the user command",
    parameters = ParseResult.serializer().descriptor,
    execute = { args -> args }
)

class ParseAgent(private val provider: LLMProvider) {
    suspend fun parse(message: String, context: ParseContext): ParseResult {
        deterministicParse(message)?.let { return it }
        val agent = AIAgent(
            provider = provider, model = AiModels.PARSE,
            systemPrompt = PARSE_SYSTEM_PROMPT,
            tools = listOf(parseResultTool),
            toolChoice = ToolChoice.Specific("return_parse_result"),
            temperature = 0.1
        )
        val response = agent.run(buildParsePrompt(message, context))
        return response.toolCalls
            .firstOrNull { it.name == "return_parse_result" }
            ?.let { Json.decodeFromJsonElement<ParseResult>(it.arguments) }
            ?: ParseResult(intent = IntentType.Unknown, confidence = 0.0,
                entities = Entities(), missingRequired = listOf("Could not parse intent"),
                safetyFlags = emptyList(), normalizedCommand = message, parserVersion = "koog-fallback-v1")
    }
}
```

### Pattern 2: Execution Planning via Tool-Calling

```kotlin
class PlanAgent(private val provider: LLMProvider, private val agentKitClient: AgentKitClient) {

    val planResultTool = Tool(
        name = "return_execution_plan",
        description = "Return the structured execution plan",
        parameters = ExecutionPlan.serializer().descriptor,
        execute = { args -> args }
    )

    suspend fun plan(parseResult: ParseResult, walletAddress: String, dryRun: Boolean): ExecutionPlan {
        // Fetch REAL gas estimate from AgentKit before plan generation
        // so the preview shows accurate USD costs
        val gasEstimate = runCatching {
            agentKitClient.estimateGas(walletAddress, null)
        }.getOrNull()

        val agent = AIAgent(
            provider = provider, model = AiModels.PLAN,
            systemPrompt = PLAN_SYSTEM_PROMPT,
            tools = listOf(planResultTool),
            toolChoice = ToolChoice.Specific("return_execution_plan"),
            temperature = 0.2
        )
        val response = agent.run(
            Json.encodeToString(PlanInput(parseResult, walletAddress, dryRun, gasEstimate))
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
class ChatSummaryAgent(private val provider: LLMProvider) {
    fun streamSummary(event: String, txResult: TxResult?): Flow<String> {
        val agent = AIAgent(provider = provider, model = AiModels.CHAT,
            systemPrompt = CHAT_SUMMARY_SYSTEM_PROMPT, maxTokens = 120, temperature = 0.7)
        return agent.streamRun(buildSummaryPrompt(event, txResult))
    }
}
```

### Pattern 4: Deterministic Fallback Parser

```kotlin
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
        return ParseResult(intent = IntentType.SendPayment, confidence = 0.95,
            entities = Entities(amount = match.groupValues[1],
                asset = match.groupValues[2].uppercase(), recipient = match.groupValues[3]),
            missingRequired = emptyList(), safetyFlags = emptyList(),
            normalizedCommand = message.trim(), parserVersion = "deterministic-v1")
    }
    SWAP.find(message)?.let { match ->
        return ParseResult(intent = IntentType.SwapAsset, confidence = 0.92,
            entities = Entities(amount = match.groupValues[1],
                fromAsset = match.groupValues[2].uppercase(),
                toAsset = match.groupValues[3].uppercase(),
                chain = match.groupValues.getOrNull(4)?.let { resolveChainId(it) }),
            missingRequired = emptyList(), safetyFlags = emptyList(),
            normalizedCommand = message.trim(), parserVersion = "deterministic-v1")
    }
    val lower = message.lowercase()
    if (BALANCE_WORDS.any { lower.contains(it) }) return ParseResult(
        intent = IntentType.CheckBalance, confidence = 0.90, entities = Entities(),
        missingRequired = emptyList(), safetyFlags = emptyList(),
        normalizedCommand = message.trim(), parserVersion = "deterministic-v1")
    if (HISTORY_WORDS.any { lower.contains(it) }) return ParseResult(
        intent = IntentType.ShowHistory, confidence = 0.88, entities = Entities(),
        missingRequired = emptyList(), safetyFlags = emptyList(),
        normalizedCommand = message.trim(), parserVersion = "deterministic-v1")
    return null
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
@Serializable
data class ParseResult(
    val intent: IntentType,
    val confidence: Double,
    val entities: Entities,
    val missingRequired: List<String>,
    val safetyFlags: List<String>,
    val normalizedCommand: String,
    val parserVersion: String
)

@Serializable
enum class IntentType {
    SendPayment, SwapAsset, StakeAsset, CheckBalance, ShowHistory, Unknown
}

@Serializable
data class Entities(
    val recipient: String? = null,
    val amount: String? = null,
    val asset: String? = null,
    val fromAsset: String? = null,
    val toAsset: String? = null,
    val chain: Long? = null,
    val slippageBps: Int? = null,
    val opportunityId: String? = null,
    val timeRange: String? = null,
    val status: String? = null
)

@Serializable
data class ExecutionPlan(
    val planId: String,
    val planType: PlanType,
    val requiresClarification: Boolean,
    val clarificationQuestions: List<ClarificationQuestion>?,
    val preview: ExecutionPreview?,
    val steps: List<ExecutionStep>,
    val policy: PolicyDecision,
    val idempotencyKey: String?,
    val expiresAt: Long
)

@Serializable
data class ExecutionPreview(
    val description: String,
    val estimatedFeeUsd: String,        // from AgentKit gas estimate (real)
    val estimatedReceivedAmount: String?,
    val priceImpactBps: Int?,
    val networkName: String
)
```

---

## KMP SSE CLIENT — PLATFORM IMPLEMENTATION

```kotlin
expect fun createSseFlow(url: String, token: String): Flow<String>

// Android & Desktop: CIO engine, chunked response streaming
// iOS: URLSession-backed engine, NSURLSession dataTask with streaming delegate
// Web (JS): callbackFlow wrapping native EventSource API
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
│   ├── database/       SQLDelight schema + queries
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
├── src/main/kotlin/...
├── Dockerfile
└── build.gradle.kts

agentkit-sidecar/ (Node.js 20)        ← NEW
├── src/
│   ├── index.ts
│   ├── agentkit.ts
│   ├── routes/
│   └── middleware/
├── package.json
├── tsconfig.json
└── Dockerfile
```

---

## AGENT SAFETY RULES

- NEVER auto-sign or bypass wallet; external wallet always has final approval
- NEVER execute ambiguous intents without clarification (confidence < 0.75)
- ALWAYS present ExecutionPreview (fees from AgentKit gas estimate + slippage + received amount) before confirm
- ALWAYS screen recipient address before building any value-moving transaction
- ALWAYS attach idempotency key to value-moving calls; reject duplicates within 24 h
- AgentKit sidecar builds unsigned calldata ONLY — it never broadcasts
- Broadcast lives in Ktor where idempotency + kill switch are enforced
- Record parser provider, model version, prompt version, and latency in telemetry

### Confidence Thresholds

| Intent type | Minimum confidence | Below threshold action |
|-------------|-------------------|----------------------|
| CheckBalance, ShowHistory | 0.60 | Clarify |
| SendPayment, SwapAsset, StakeAsset | 0.75 | Clarify |
| Unknown | — | Always clarify |

---

## PHASE 1: Setup, DI, Core Network, Root Shell

**Sprint target**: 2 developer-days (1 backend, 1 client — can overlap)

Includes standing up the AgentKit sidecar skeleton so Ktor can health-check it from day one.

### Backend: Ktor Module Bootstrap

```kotlin
fun Application.module() {
    configureDI()
    configureDatabaseFactory()
    configureKoog()
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureRateLimiting()
    configureErrorHandling()
    configureAgentKitClient()    // [NEW] validate sidecar health on startup
    configureRouting()
}
```

### AgentKit Sidecar: Phase 1 Deliverable

Sidecar runs, health endpoint responds, `internalOnly` middleware rejects requests without `x-sidecar-secret`. Only `/health` and the skeleton routes need to exist — full action implementations come in Phase 4.

### Environment Config (HOCON)

```hocon
ktor {
    deployment { port = 8080, port = ${?PORT} }
    application { modules = [ com.letapay.ApplicationKt.module ] }
}
openai { apiKey = ${OPENAI_API_KEY} }
firebase { serviceAccountJson = ${?FIREBASE_SA_JSON} }
coinbase { apiKey = ${COINBASE_API_KEY}, riskApiKey = ${COINBASE_RISK_KEY} }
database { url = ${DATABASE_URL}, maxPoolSize = 10 }
security { sessionSecret = ${SESSION_SECRET}, refreshTokenPepper = ${REFRESH_TOKEN_PEPPER} }
rateLimit { aiParsePm = 60, aiPlanPm = 20 }
killSwitch { valueMoves = false, valueMoves = ${?KILL_SWITCH_VALUE_MOVES} }
agentkit { sidecarUrl = ${?AGENTKIT_SIDECAR_URL}, sidecarSecret = ${SIDECAR_SECRET} }
```

### Database Schema (Exposed Tables)

```kotlin
object Sessions : Table("sessions") {
    val id = varchar("id", 36)
    val walletAddress = varchar("wallet_address", 42)
    val refreshTokenHash = text("refresh_token_hash")
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

### Feature Flags

```kotlin
data class FeatureFlags(
    val YIELD_ENABLED: Boolean = false,
    val SWAP_ENABLED: Boolean = false,
    val VOICE_INPUT_ENABLED: Boolean = false,
    val BASE_STAKING_ENABLED: Boolean = false,
    val ENS_RESOLUTION_ENABLED: Boolean = false,
    val USD_VALUE_ENABLED: Boolean = true,
    val KILL_SWITCH_VALUE_MOVES: Boolean = false,
    val AGENTKIT_ENABLED: Boolean = true        // [NEW] kill switch for sidecar specifically
)
```

### Phase 1 Deliverable

All 4 platforms build. Ktor backend builds and starts. AgentKit sidecar starts and Ktor health-checks it at startup. Koin graphs resolve on both sides. `/ai/parse` returns a valid `ParseResult` for "send 0.1 ETH to 0x1234...".

---

## PHASE 2: WalletConnect & Session Management

**Sprint target**: 2 developer-days (overlap with Phase 1)

No changes from v5. Auth routes, SIWE verification via Web3j, refresh token rotation unchanged.

### Auth Routes

```kotlin
fun Route.authRoutes(authService: AuthService) {
    post("/auth/request-nonce") { /* generate UUID v4 nonce, 5 min TTL */ }
    post("/auth/verify-signature") { /* SIWE verify, issue session+refresh+firebase tokens */ }
    post("/auth/refresh-token") { /* rotate refresh token, return fresh firebase token too */ }
    post("/auth/firebase-token") { authenticate("session-auth") { /* mint fresh firebase token */ } }
    post("/auth/revoke-session") { authenticate("session-auth") { /* revoke by session ID */ } }
}
```

### Phase 2 Deliverable

Connect wallet → session tokens stored → balances visible → session survives app restart.

---

## PHASE 3: Chat Scaffolding & Firebase Custom Auth

**Sprint target**: 2 developer-days (overlap with Phase 2)

No changes from v5. Firebase security rules, offline message queue unchanged.

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

### Phase 3 Deliverable

Chat UI functional. Firebase authenticated with wallet identity. Offline queue persists and retries. SSE tokens stream to chat bubble.

---

## PHASE 4: Transaction Intent & Send Payment (AgentKit-powered)

**Sprint target**: 3 developer-days

### Send Flow (updated with AgentKit sidecar)

```
User: "Send 25 USDC to vitalik.eth on Polygon"

1. DeterministicParser → intent=SendPayment, amount=25, asset=USDC, recipient=vitalik.eth
2. PolicyGuard: chain=137 (USDC default), ENS_RESOLUTION_ENABLED → EnsService.resolve("vitalik.eth")
3. ScreeningService.check(resolvedAddress) → pass
4. [NEW] AgentKitClient.estimateGas(walletAddress, networkId="polygon-mainnet") → real gas figure
5. POST /ai/plan → ExecutionPreview with REAL MATIC gas + USD cost (from AgentKit)
6. User confirms
7. POST /transactions/build
   → [NEW] AgentKitClient.buildTransfer(fromAddress, resolvedAddress, "USDC", "25", "polygon-mainnet")
   → AgentKit sidecar returns unsigned ERC-20 calldata
   → Ktor returns to client
8. WalletConnect signs → client returns signed hex
9. POST /transactions/send {signedTx, idempotencyKey}
   → Ktor broadcasts via CDP Polygon RPC (NOT sidecar)
10. Poll /transactions/status/{txHash}
11. GET /ai/chat-stream → stream "✓ Sent 25 USDC to vitalik.eth on Polygon..."
```

### ENS Resolution Service

```kotlin
class EnsService(private val httpClient: HttpClient, private val cache: Cache<String, String>) {
    private val ENS_REGISTRY = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
    private val ETH_RPC = "https://eth-mainnet.g.alchemy.com/v2/${config.alchemyKey}"

    suspend fun resolve(ensName: String): String? {
        if (!ensName.endsWith(".eth")) return null
        cache.getIfPresent(ensName)?.let { return it }
        val namehash = computeNamehash(ensName)
        val result = httpClient.post(ETH_RPC) {
            setBody(JsonRpcRequest("eth_call", listOf(
                mapOf("to" to ENS_REGISTRY, "data" to "0x3b3b57de$namehash"), "latest"
            )))
        }.body<JsonRpcResponse>()
        val address = result.result?.takeIf { it != "0x" + "0".repeat(64) }
            ?.let { "0x" + it.takeLast(40) }
        address?.let { cache.put(ensName, it) }
        return address
    }
}
```

### Transaction Routes (updated)

```kotlin
fun Route.transactionRoutes(txService: TransactionService, screening: ScreeningService, agentKit: AgentKitClient) {
    authenticate("session-auth") {
        post("/transactions/build") {
            killSwitchGuard()
            val request = call.receive<BuildRequest>()
            val principal = call.principal<WalletPrincipal>()!!
            if (!screening.check(request.to)) throw AddressRejectedError(request.to)

            // [NEW] Delegate calldata construction to AgentKit sidecar
            val calldata = agentKit.buildTransfer(
                fromAddress = principal.walletAddress,
                toAddress = request.to,
                asset = request.asset,
                amount = request.amount,
                networkId = request.networkId
            )
            call.respond(calldata)
        }

        post("/transactions/send") {
            killSwitchGuard(); idempotencyGuard()
            val request = call.receive<SendRequest>()
            val txHash = txService.broadcastSigned(request) // direct CDP RPC, not sidecar
            call.respond(SendResponse(txHash = txHash, status = "submitted"))
        }

        get("/transactions/status/{txHash}") { /* poll CDP */ }
        get("/transactions/history") { /* CDP Onchain Data */ }
    }
}
```

### Phase 4 Deliverable

Send via chat → ENS resolved → screened → AgentKit builds calldata → WalletConnect signs → broadcast → confirmed → streaming summary. Real gas estimates shown in USD.

---

## PHASE 5: Trading / Swaps (AgentKit-powered)

**Sprint target**: 2 developer-days

### Quote Lifecycle (via AgentKit sidecar)

```
1. POST /swap/quote {fromAsset, toAsset, amount, networkId, slippageBps}
   → Ktor calls AgentKitClient.getSwapQuote(...)
   → Sidecar invokes AgentKit swapActionProvider (CDP-backed)
   → Returns: toAmount, rate, priceImpactBps, expiresAt (60 s TTL)

2. Client shows preview: "Receive ~X USDC | Price impact 0.12% | Fee $0.42"

3. User confirms → POST /swap/execute {quoteParams, idempotencyKey}
   → Ktor calls AgentKitClient.buildSwap(...)
   → Sidecar returns unsigned swap calldata
   → WalletConnect signs → POST /transactions/send (reuses Ktor broadcast route)

4. On confirmation → streaming summary
```

### Swap Routes

```kotlin
fun Route.swapRoutes(agentKit: AgentKitClient, screening: ScreeningService) {
    authenticate("session-auth") {
        post("/swap/quote") {
            checkRateLimit(...)
            val request = call.receive<SwapQuoteRequest>()
            val quote = agentKit.getSwapQuote(request)
            call.respond(quote)
        }

        post("/swap/execute") {
            killSwitchGuard(); idempotencyGuard()
            val request = call.receive<SwapExecuteRequest>()
            val principal = call.principal<WalletPrincipal>()!!
            if (!screening.check(request.toAddress)) throw AddressRejectedError(request.toAddress)
            val calldata = agentKit.buildSwap(request.toSwapQuoteRequest(principal.walletAddress))
            call.respond(calldata) // client signs + broadcasts
        }
    }
}
```

### Quote Expiry Handling

Quotes expire in 60 seconds. Client shows countdown timer. On expiry, client re-fetches automatically (up to 3 times) then prompts manual retry.

### Phase 5 Deliverable

Swap via chat or manual UI. AgentKit provides real quotes and unsigned calldata. Quote expiry handled gracefully. Slippage protection enforced at sidecar level.

---

## PHASE 6: Staking / Yield (AgentKit-powered)

**Sprint target**: 2 developer-days

### MVP Staking Scope

| Chain | Provider | Protocol | AgentKit action |
|-------|----------|----------|-----------------|
| Ethereum | Lido | stETH | `stake_eth` via CDP contract call |
| Polygon | AAVE | aTokens | `deposit` via AAVE LendingPool |

```
1. POST /yield/opportunities → list of active staking positions + current APY
2. POST /yield/stake {opportunityId, amount, idempotencyKey}
   → [NEW] Ktor calls AgentKitClient.buildStake(opportunityId, amount, walletAddress)
   → Sidecar builds stake calldata → WalletConnect signs → broadcast
3. POST /yield/unstake {positionId, amount, idempotencyKey}
   → AgentKit handles Lido withdrawal queue + AAVE immediate withdrawal
4. GET /yield/positions → current positions with live USD value
```

### Stake Routes

```kotlin
fun Route.yieldRoutes(agentKit: AgentKitClient, screening: ScreeningService) {
    authenticate("session-auth") {
        get("/yield/opportunities") { /* return Lido APY + AAVE APY */ }

        post("/yield/stake") {
            killSwitchGuard(); idempotencyGuard()
            val request = call.receive<StakeRequest>()
            val principal = call.principal<WalletPrincipal>()!!
            val calldata = agentKit.buildStake(request.copy(walletAddress = principal.walletAddress))
            call.respond(calldata)
        }

        post("/yield/unstake") {
            killSwitchGuard(); idempotencyGuard()
            val request = call.receive<UnstakeRequest>()
            val principal = call.principal<WalletPrincipal>()!!
            val calldata = agentKit.buildUnstake(request.copy(walletAddress = principal.walletAddress))
            call.respond(calldata)
        }

        get("/yield/positions") { /* poll DB + CDP for live positions */ }
    }
}
```

### Phase 6 Deliverable

Stake/unstake via chat and manual UI. AgentKit builds calldata. Positions show live. Lido unbonding state handled.

---

## PHASE 7: Push Notifications & Error Handling

**Sprint target**: 2 developer-days. No changes from v5.

```kotlin
class ConfirmationWatcher(private val db: Database, private val fcm: FirebaseMessaging) {
    suspend fun watch() = coroutineScope {
        while (isActive) {
            val pending = db.getPendingWatchedTxs()
            pending.forEach { tx -> launch { /* poll status → push notification */ } }
            delay(15_000)
        }
    }
}
```

### Error Envelope

```kotlin
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val retryAfter: Int? = null
)
```

New error code added: `AGENTKIT_UNAVAILABLE` — returned when sidecar health check fails, with fallback behavior (show manual input form; disable AI-driven builds).

---

## PHASE 8: Polish, Testing, Performance

**Sprint target**: 2 developer-days

### Required Test Coverage

| Layer | Test type | Minimum coverage |
|-------|-----------|-----------------|
| DeterministicParser | Unit | 100% of regex patterns |
| ParseAgent | Unit (mock LLM) | All 6 intent types |
| AuthService | Unit | Nonce generation, signature verify, token rotation, replay detection |
| AgentKitClient | Unit (mock sidecar) | Transfer build, swap quote, swap build, stake build, gas estimate |
| AgentKit sidecar routes | Integration (supertest) | Transfer, swap quote, swap build, internal-only guard |
| TransactionRoutes | Integration (testApplication) | Build, send, kill switch, idempotency |
| SwapRoutes | Integration | Quote, execute, expiry |
| E2E (Sepolia / Base Sepolia testnet) | Manual | Send ETH, swap USDC, stake, check history |

### Performance

- AgentKit sidecar: pool of pre-initialized AgentKit instances per networkId (avoid cold init per request)
- Ktor: circuit breaker wrapping `AgentKitClient` — if sidecar is down, degrade gracefully to error with `AGENTKIT_UNAVAILABLE` code
- Price feed: Redis cache 30–60 s TTL
- AI response cache: identical deterministic parses cached 60 s (LRU in-memory)

---

## PHASE 9: Deployment

**Sprint target**: 2 developer-days

### Docker Compose (local dev + Railway/Fly.io)

```yaml
# docker-compose.yml
version: "3.9"
services:
  ktor-backend:
    build: ./backend-ktor
    ports: ["8080:8080"]
    environment:
      - DATABASE_URL=postgresql://postgres:postgres@postgres:5432/letapay
      - AGENTKIT_SIDECAR_URL=http://agentkit-sidecar:3100
      - SIDECAR_SECRET=${SIDECAR_SECRET}
      # ... other env vars
    depends_on: [postgres, redis, agentkit-sidecar]

  agentkit-sidecar:
    build: ./agentkit-sidecar
    # NOT exposed on host — internal Docker network only
    environment:
      - CDP_API_KEY_ID=${CDP_API_KEY_ID}
      - CDP_API_KEY_SECRET=${CDP_API_KEY_SECRET}
      - CDP_WALLET_SECRET=${CDP_WALLET_SECRET}
      - SIDECAR_SECRET=${SIDECAR_SECRET}
    ports: [] # No public port binding — internal only

  postgres:
    image: postgres:16
    environment: { POSTGRES_DB: letapay, POSTGRES_PASSWORD: postgres }

  redis:
    image: redis:7-alpine
```

### Ktor Dockerfile

```dockerfile
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
| Web | Static hosting (Netlify/Vercel) |
| Desktop | Platform installers via jpackage |
| Ktor backend | Railway or Fly.io (MVP) → AWS ECS / GCP Cloud Run |
| AgentKit sidecar | **Same Railway/Fly.io app — separate service, internal-only** |

### Fly.io Multi-Service Setup

```toml
# fly.toml
app = "letapay"
primary_region = "iad"

[[services]]
  internal_port = 8080
  protocol = "tcp"
  [services.concurrency]
    hard_limit = 25

# AgentKit sidecar: no external handler — Fly internal network only
# Deploy as separate Fly app: letapay-agentkit (private networking via .internal DNS)
```

---

## ENVIRONMENT VARIABLES

| Variable | Used by | Notes |
|----------|---------|-------|
| `OPENAI_API_KEY` | Koog provider (Ktor) | Server-side only |
| `AI_PARSE_MODEL` | ParseAgent (Ktor) | Default: `gpt-4o-mini` |
| `AI_PLAN_MODEL` | PlanAgent (Ktor) | Default: `gpt-4o` |
| `FIREBASE_SA_JSON` | Firebase Admin SDK (Ktor) | Service account JSON |
| `COINBASE_API_KEY` | CoinbaseService (Ktor) | Price feed, tx history |
| `COINBASE_RISK_KEY` | ScreeningService (Ktor) | Risk Assessment API |
| `CDP_API_KEY_ID` | AgentKit sidecar | **Sidecar only** — never in Ktor |
| `CDP_API_KEY_SECRET` | AgentKit sidecar | **Sidecar only** |
| `CDP_WALLET_SECRET` | AgentKit sidecar | **Sidecar only** |
| `AGENTKIT_SIDECAR_URL` | AgentKitClient (Ktor) | e.g. `http://agentkit-sidecar:3100` |
| `SIDECAR_SECRET` | Ktor + Sidecar | Shared secret for internal auth |
| `KILL_SWITCH_VALUE_MOVES` | KillSwitch (Ktor) | Default: `false` |
| `SESSION_SECRET` | JWT signer (Ktor) | Min 32 bytes random |
| `REFRESH_TOKEN_PEPPER` | Argon2id (Ktor) | 32 bytes random |
| `DATABASE_URL` | HikariCP (Ktor) | PostgreSQL connection string |
| `ALCHEMY_API_KEY` | EnsService + RPC (Ktor) | Server-side only |
| `REDIS_URL` | Rate limiter (Ktor) | Optional; in-memory fallback |

---

## DEPENDENCY VERSIONS (VERSION CATALOG)

```toml
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
# Sidecar (package.json)
# @coinbase/agentkit = "^0.7.4"
# express = "^4.19.0"

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

## SECURITY THREAT MODEL (Web3-Specific + AgentKit additions)

| Threat | Mitigation |
|--------|-----------|
| Signature replay | Nonces single-use; SIWE includes expiry timestamp |
| Address poisoning | Show first 6 + last 4 chars always; full address on tap |
| Drainer contracts | Address screening via Coinbase Risk Assessment before every value move |
| Front-running (swap) | Slippage tolerance capped at 1000 bps; plan shows priceImpactBps |
| Refresh token theft | Argon2id + pepper; rotation; reuse detection revokes family |
| AI prompt injection | Parse input sanitized; system prompt sandboxed; structured output rejects free-form injection |
| Kill switch bypass | Checked server-side in Ktor on every value-moving route |
| Session fixation | New session ID on every auth; old tokens invalidated |
| Excessive API costs (AI) | Per-wallet rate limit (60 parse/min, 20 plan/min); deterministic parser handles most cases |
| AgentKit sidecar direct access | No public port; `x-sidecar-secret` shared secret; Docker internal network only |
| CDP key exposure via sidecar | CDP keys are sidecar-only env vars; Ktor has no CDP private key access |
| Sidecar SSRF | Sidecar only makes outbound calls to Coinbase CDP domains; egress restricted in Fly.io |
| AgentKit builds malicious calldata | Ktor performs address screening BEFORE calling sidecar; sidecar output is unsigned and shown to user before wallet signs |

---

## VERIFICATION CHECKLIST

- [ ] Ktor backend builds as Docker image
- [ ] AgentKit sidecar builds as Docker image (`docker build -t letapay-sidecar ./agentkit-sidecar`)
- [ ] Docker Compose starts both services; Ktor health-checks sidecar at startup
- [ ] Sidecar rejects requests without `x-sidecar-secret` header with 403
- [ ] Sidecar is NOT reachable from the public internet (no host port binding)
- [ ] All 4 client platforms build against Ktor backend (localhost:8080 in dev)
- [ ] Koin DI resolves on both backend and client
- [ ] `/auth/request-nonce` + `/auth/verify-signature` flow works end-to-end
- [ ] Refresh token rotation works; reuse detection revokes family
- [ ] `/ai/parse` returns valid `ParseResult` for "Send 0.1 ETH to vitalik.eth"
- [ ] `/ai/plan` returns `ExecutionPreview` with REAL gas figures (from AgentKit gas estimate)
- [ ] `/transactions/build` returns AgentKit unsigned calldata for ETH transfer
- [ ] `/swap/quote` returns AgentKit swap quote with priceImpactBps
- [ ] `/swap/execute` returns AgentKit unsigned swap calldata
- [ ] `/yield/stake` returns AgentKit Lido/AAVE unsigned stake calldata
- [ ] `/ai/chat-stream` streams via Ktor SSE; KMP SSE client appends tokens to chat bubble
- [ ] ENS resolution: "vitalik.eth" resolves to canonical address (feature-flagged)
- [ ] Address screening rejects known high-risk addresses
- [ ] Kill switch returns 503 for all value-moving endpoints when flag active
- [ ] Idempotency keys block duplicate sends within 24 h
- [ ] Rate limiter returns 429 + `Retry-After` when threshold exceeded
- [ ] Full send flow on Sepolia + Base Sepolia: chat → parse → plan → AgentKit build → WalletConnect sign → broadcast → streaming summary
- [ ] Firebase chat reconnects after custom token expiry
- [ ] `AGENTKIT_UNAVAILABLE` error degrades gracefully (manual input form shown)
- [ ] Spotless passes for all Kotlin source; ESLint passes for sidecar TypeScript

---

## DEFERRED (POST-MVP)

- Group chats
- Voice input (`VOICE_INPUT_ENABLED` flag ready)
- Biometric gating for transaction approval
- WebSocket transport replacing Firebase for chat
- Base chain staking
- MCP tool integrations (Koog has built-in MCP support; AgentKit has MCP server support)
- Hardware wallet support (Ledger via WalletConnect)
- Multi-account support
- Token allowlist expansion
- Fiat on-ramp (AgentKit Onramp action provider is available — enable post-MVP)
- AgentKit `x402` micropayment protocol integration
- AgentKit `deployToken` / `deployNFT` actions
- AgentKit gasless transactions via Smart Wallet (removes need for user to hold ETH for gas)
- Notification preference management UI
- Analytics / event tracking
- Mainnet deployment (MVP ships on testnets)