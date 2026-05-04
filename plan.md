# Plan: Leta Pay Reality-Synced Roadmap (Phase 15)

Last updated: 2026-05-04

## 1. Product State

Leta Pay MVP is feature-complete for the canonical architecture:

- KMP clients (Android, iOS, Desktop, Web) for chat-first wallet UX
- Ktor backend for auth, policy enforcement, idempotency, kill switch, and transaction lifecycle
- Node.js AgentKit sidecar for unsigned action construction over internal HTTP only
- Postgres + Redis as persistence and coordination services

This plan supersedes legacy template assumptions (reusable actionhub workflows, template sync jobs, and managed deployment defaults).

## 2. Locked Architecture

### 2.1 Service Boundaries

- Public edge:
  - Web frontend (static assets served by Nginx)
  - Ktor backend API
- Internal network:
  - AgentKit sidecar (`agentkit-sidecar:3100`), never publicly exposed
  - Postgres
  - Redis

### 2.2 Trust Model

- Non-custodial: users sign with external wallets via WalletConnect
- Ktor is the policy boundary for all value-moving requests
- Sidecar builds unsigned calldata and quotes only
- No private keys stored in backend or sidecar

### 2.3 Backend-to-Sidecar Contract

- Transport: internal HTTP
- Auth: `x-sidecar-secret` header
- Endpoint style:
  - `/agentkit/transfer/*`
  - `/agentkit/swap/*`
  - `/agentkit/stake/*`
  - `/agentkit/gas/*`
  - `/agentkit/address/screen`
  - `/agentkit/balance/*`

## 3. Data and Safety Invariants

### 3.1 Database and State

- Postgres is source of truth for sessions, transaction records, and staking lifecycle metadata
- Redis provides transient coordination (rate limits, short-lived operational state)
- Firebase custom auth is used for chat-linked identity flows

### 3.2 Idempotency

- Required for value-moving operations
- Client supplies UUID v4 key via `Idempotency-Key`
- Backend enforces one-time processing window and deterministic replay behavior

### 3.3 Kill Switch

- `KILL_SWITCH_VALUE_MOVES=true` blocks value-moving routes server-side
- Must be checked before sidecar invocation and before signed tx submission

## 4. Delivery and Operations Model

### 4.1 CI Validation Commands

CI and local validation are based on repository-native commands:

- `npm ci` and `npm run build` in `agentkit-sidecar/`
- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:spotlessCheck`
- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:detekt`
- `./gradlew --no-daemon --no-configuration-cache :backend-ktor:test`
- `./gradlew --no-daemon --no-configuration-cache :cmp-web:compileKotlinJs`
- `./gradlew --no-daemon --no-configuration-cache :cmp-web:jsBrowserDistribution`

### 4.2 Hosting Strategy

Primary deployment target is VPS self-hosting with Docker Compose:

- `docker-compose.prod.yml` runs postgres, redis, ktor-backend, agentkit-sidecar, web-frontend
- Web frontend serves `:cmp-web:jsBrowserDistribution` artifacts using Nginx
- Reverse proxy and TLS handled by Caddy or Nginx + Certbot

Managed-host-specific assumptions (Fly.io, Vercel defaults) are not canonical for production.

## 5. Current Phase Status

### Completed

- Core chat wallet flows wired to Ktor + sidecar architecture
- Security model: non-custodial signing + backend policy enforcement
- Environment contracts documented for backend and sidecar
- CI workflows rewritten to native repository commands
- VPS production compose topology defined

### Active

- Production VPS rollout hardening (secrets injection, backups, monitoring)
- Documentation reality sync and template cruft removal
- AI workflow prompt and rule synchronization under `.agent/`

### Next

- Add production observability baseline (metrics + alert thresholds)
- Add automated backup and restore verification for Postgres volumes
- Add deployment smoke-test script for post-upgrade checks

## 6. Engineering Rules (Canonical)

- Backend is Ktor; sidecar is Node.js/AgentKit; communication is internal HTTP only
- Compose Multiplatform + UDF patterns remain default frontend architecture
- Offline-first repositories and SQLDelight-backed local state remain required on clients
- Frontend domain error mapping must match backend machine error codes exactly
- Build commands must use `--no-daemon --no-configuration-cache` and run serially for deterministic agent execution
- No placeholders, no mock data, and no `TODO` markers are allowed in committed repository changes
