# Manager Rules — Leta Pay Canonical Constitution

This file is the mandatory operating contract for all AI agents in this repository.

## 1. Canonical Architecture

- Backend is Kotlin Ktor (`backend-ktor`).
- Sidecar is Node.js + Coinbase AgentKit (`agentkit-sidecar`).
- Backend and sidecar communicate over internal HTTP only using `x-sidecar-secret`.
- Sidecar never serves public internet traffic and never owns user custody keys.
- Ktor is the public policy boundary for auth, rate limits, idempotency, and kill-switch enforcement.

## 2. Frontend Architecture Rules

- UI stack is Compose Multiplatform across Android, iOS, Desktop, and Web.
- State management must follow Unidirectional Data Flow.
- Repositories are offline-first and SQLDelight-backed where local persistence applies.
- Frontend domain errors must map exactly to backend machine error codes.
- No platform-specific code in `commonMain`.

## 3. Build and Validation Rules

- Run build and validation tasks serially.
- Every Gradle command must include:
  - `--no-daemon`
  - `--no-configuration-cache`
- Required validation baseline:
  - `./gradlew --no-daemon --no-configuration-cache :backend-ktor:spotlessCheck`
  - `./gradlew --no-daemon --no-configuration-cache :backend-ktor:detekt`
  - `./gradlew --no-daemon --no-configuration-cache :backend-ktor:test`
  - `./gradlew --no-daemon --no-configuration-cache :cmp-web:compileKotlinJs`
  - `./gradlew --no-daemon --no-configuration-cache :cmp-web:jsBrowserDistribution`
  - `cd agentkit-sidecar && npm ci && npm run build`

## 4. Zero Placeholder Policy

- Never commit placeholder code.
- Never leave `TODO`, `FIXME`, mock payloads, fake secrets, or fake API responses in committed changes.
- If a requirement is blocked, report the blocker explicitly instead of stubbing behavior.

## 5. Security and Custody Rules

- Never store private keys or seed phrases.
- Keep WalletConnect signing external to Leta Pay services.
- Treat `KILL_SWITCH_VALUE_MOVES` as mandatory control for value-moving routes.
- Preserve backend idempotency guarantees for all value-moving operations.

## 6. Delivery Protocol

Every handoff must include:

- Scope completed
- Files changed
- Commands executed
- Validation result
- Remaining risks or blockers
