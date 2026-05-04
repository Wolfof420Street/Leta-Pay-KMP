# LetaPay

[![Leta Pay CI](https://github.com/Wolfof420Street/Leta-Pay-KMP/actions/workflows/pr-check.yml/badge.svg)](https://github.com/Wolfof420Street/Leta-Pay-KMP/actions/workflows/pr-check.yml)
![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Desktop](https://img.shields.io/badge/Platform-Desktop-0078D4)
![Web](https://img.shields.io/badge/Platform-Web-F7DF1E?logo=javascript&logoColor=000)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](LICENSE)

LetaPay is an enterprise-grade, non-custodial crypto wallet platform that combines conversational AI intent orchestration with deterministic transaction controls. It is built to support high-confidence value movement while preserving strict security boundaries: wallets sign externally, the backend controls policy and replay safety, and the sidecar only produces unsigned calldata.

The stack is deliberately split into specialized layers: Kotlin Multiplatform clients for Android/Web/Desktop UX, a Ktor backend for auth and transaction governance, and a Node.js AgentKit sidecar for blockchain execution planning. This architecture gives product teams AI-powered user flows without sacrificing auditability, idempotency, or kill-switch control.

## Architecture Overview

LetaPay runs as a 3-tier system:

1. KMP Frontend: cross-platform app that drives chat intents, preview UX, WalletConnect signing, and portfolio/history views.
2. Ktor Backend (Auth + Orchestration): verifies wallet identity, enforces kill switch and idempotency, validates/sanitizes requests, and owns all client-facing APIs.
3. Node.js AgentKit Sidecar: internal-only execution builder that talks to Coinbase CDP/AgentKit and returns unsigned calldata payloads.

For deep architecture docs, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Feature Matrix

### Supported Chains

- [x] Ethereum Mainnet (`1`)
- [x] Polygon Mainnet (`137`)
- [x] Base Mainnet (`8453`)

### AI / Intent Coverage

- [x] Send / transfer intent parsing
- [x] Swap intent parsing + quote/execute pipeline
- [x] Stake intent parsing + opportunity/position flow
- [x] Balance and history intent handling
- [x] Preview-plan-confirm lifecycle before signing

### Wallet Capabilities

- [x] Non-custodial external signing via WalletConnect
- [x] SIWE-style nonce + signature authentication
- [x] JWT session lifecycle with refresh controls
- [x] Firebase custom token bootstrap for chat features
- [x] Unsigned transaction preview prior to broadcast

## Quick Start

- Architecture deep-dive: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- API contracts and payload schemas: [docs/API_REFERENCE.md](docs/API_REFERENCE.md)
- Environment variables (backend/sidecar/frontend): [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md)
- Local dev runbook (Docker + Android/Desktop/Web): [docs/LOCAL_DEV.md](docs/LOCAL_DEV.md)

## Repository Components

- `backend-ktor/` - Ktor backend (auth, orchestration, API, idempotency, kill switch)
- `agentkit-sidecar/` - Node.js AgentKit sidecar (internal unsigned transaction builder)
- `cmp-android/` - Android target
- `cmp-desktop/` - Desktop JVM target
- `cmp-web/` - Web target
- `core/*`, `feature/*`, `cmp-shared/`, `cmp-navigation/` - shared KMP domain/data/ui stack
