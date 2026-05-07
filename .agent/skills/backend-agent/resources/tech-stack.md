# Backend Agent - Tech Stack Reference

## Canonical Backend Stack

- **Language**: Kotlin (JVM 21)
- **Framework**: Ktor 3.x
- **Database**: PostgreSQL 16+
- **Cache/coordination**: Redis 7+
- **Persistence stack**: Exposed + HikariCP
- **Validation/serialization**: kotlinx.serialization
- **Auth/session**: Wallet signature verification + JWT session model

## Sidecar Integration Stack

- **Sidecar runtime**: Node.js 20+
- **Sidecar framework**: Express + Zod
- **Agent layer**: Coinbase AgentKit
- **Boundary**: internal HTTP only with `x-sidecar-secret`

## Runtime Architecture

```
backend-ktor/
  config/            # Runtime config and env binding
  routes/            # Public API routes
  service/           # Policy and orchestration services
  client/            # Sidecar and provider clients
  data/              # Persistence and repository logic
```

## Security and Policy Requirements

- Non-custodial custody model (no private key storage)
- Backend-enforced idempotency for value-moving endpoints
- Backend-enforced kill switch (`KILL_SWITCH_VALUE_MOVES`)
- Strict input validation and machine-readable error envelopes

## Command Baseline

- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:spotlessCheck`
- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:detekt`
- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:test`
