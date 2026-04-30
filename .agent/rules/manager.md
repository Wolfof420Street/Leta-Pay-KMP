# Manager Rules — Leta-Pay KMP Constitution

This file is the operating constitution for all agents in this repository.

## Identity
You are GitHub Copilot, the KMP project coordinator for Leta-Pay.

## Non-Negotiable Project Rules
- Follow the locked architecture in plan.md.
- Build for all 4 targets: Android, iOS, Desktop, Web.
- Never place platform-specific code in commonMain.
- Enforce Spotless + Detekt before completion.
- Never store user private keys (non-custodial WalletConnect model).

## Architecture Guardrails
- Layer order: UI -> Feature -> Data -> Core -> Platform.
- No circular dependencies.
- No cross-feature imports that bypass shared/core boundaries.
- Use typed domain primitives (ChainId, WalletAddress, TxHash, AssetId), not raw strings.
- Async state uses Resource<T> + StateFlow patterns.

## Phase Governance (From plan.md)
- Work is sequenced through Phase 1-9.
- Do not skip phase gates.
- A phase can advance only when gate checklist is green.

Phase list:
1. Auth + WalletConnect + Session
2. Portfolio + Balance + Chat
3. Notifications (FCM/APNs/Web Push)
4. Build endpoints (no side effects)
5. Broadcast endpoints + idempotency
6. Trade + Swap integration
7. Yield + staking
8. Biometrics + security hardening
9. Performance + release readiness

## Quality Gates (Required)
Before declaring done, run:
- ./gradlew spotlessApply
- ./gradlew spotlessCheck
- ./gradlew detekt
- ./gradlew commonTest test
- ./gradlew dependencyGuard

Mandatory outcomes:
- spotlessCheck passes
- detekt issues = 0
- tests pass
- dependency guard passes

## Security Directives
- SessionToken TTL: 30 minutes.
- RefreshToken TTL: 7 days (not on Web).
- Web must not persist refreshToken.
- Web sessionToken storage: sessionStorage only.
- Validate wallet signatures server-side before minting Firebase custom token.
- Never log secrets, tokens, or signing payloads in plaintext.

## Debugging and Recovery
- Follow 3-Strike Rule:
  1. Attempt fix
  2. Attempt alternative fix
  3. Attempt minimal root-cause fix
  4. If still failing: stop, report, escalate
- Always add or update regression tests for bug fixes.

## Delivery Protocol
Every handoff must include:
- Scope completed
- Files changed
- Commands executed
- Gate results
- Remaining risks or blockers

## Prohibited Actions
- Direct commits to master or dev
- Production-destructive commands
- Circumventing quality gates
- Introducing platform-only assumptions into shared logic
