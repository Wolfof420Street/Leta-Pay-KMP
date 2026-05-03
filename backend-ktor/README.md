# backend-ktor

Ktor service that owns public API, auth, orchestration, and transaction lifecycle state.

## Responsibilities

- Wallet auth flow: nonce issuance, SIWE verification, JWT issuance, refresh handling.
- Orchestration layer: deterministic/AI intent processing and routing.
- Idempotent transaction mutation endpoints.
- Broadcast/state tracking and SSE-style summary support.
- Safety controls: rate limits, kill switch, validation, and normalized error envelopes.

## Key Production Controls

### Auth
- Session auth middleware validates JWT/session principal on protected routes.
- Nonce replay prevention and refresh-token family revocation are enforced.
- Firebase custom token minting is part of authenticated app bootstrap.

### Orchestration
- Transaction build routes screen recipients and normalize outbound transaction payloads.
- Backend calls AgentKit sidecar through internal routes only.
- Client-facing contract remains stable where possible; updated payloads include unsigned tx shape.

### Idempotency
- Mutating routes require `Idempotency-Key`.
- First successful response is persisted and replayed for duplicate keys.
- Conflicting payloads under same key return conflict error.

### Kill Switch
- Value-moving endpoints are guarded by a kill switch.
- Active switch returns service unavailable with a machine-readable code.

## Error Contract

All errors return a consistent envelope:

```json
{
  "code": "MACHINE_CODE",
  "message": "Human readable message",
  "retryAfter": 30
}
```

Examples: `KILL_SWITCH_ACTIVE`, `RATE_LIMIT_EXCEEDED`, `INVALID_TX_HASH`, `AGENTKIT_UNAVAILABLE`.

## Local Run

```bash
./gradlew --no-daemon :backend-ktor:run --no-configuration-cache
```
