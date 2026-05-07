# Data Model (Backend Runtime)

This document summarizes the runtime data model for the Ktor backend and how idempotency and kill-switch policies are enforced.

## Core Storage Components

- Postgres: system of record for durable data
- Redis: transient state for rate limits, short-lived coordination, and operational flags

## Primary Domain Records

### Session and Auth Records

- Wallet identity session state
- JWT/refresh token family metadata
- SIWE nonce challenge and replay tracking

### Transaction Lifecycle Records

- Unsigned build request metadata
- Signed submission metadata
- Broadcast result status (`submitted`, `pending`, `confirmed`, `failed`)
- Chain, asset, amount, and destination context for auditability

### Yield and Staking Records

- Opportunity snapshot references
- Position lifecycle (`active`, `closed`, `error`)
- Accrued rewards and valuation snapshots

## Idempotency Design

- Input key: client-provided UUID v4 (`Idempotency-Key`)
- Scope: value-moving operations (`/transactions/build`, `/transactions/send`, `/swap/*`, `/yield/*`)
- Behavior:
  - First request with new key: process and persist normalized response metadata
  - Retry with same key and same semantic request: deterministic replay response
  - Retry with same key and conflicting semantic payload: `IDEMPOTENCY_CONFLICT` (409)

Implementation note:
- Durable idempotency metadata is persisted in backend storage.
- TTL policy is enforced to prevent unbounded key growth while preserving retry safety windows.

## Kill-Switch Design

- Runtime flag: `KILL_SWITCH_VALUE_MOVES`
- Enforced at backend boundary before sidecar call and before signed tx submission
- When enabled, value-moving endpoints return `KILL_SWITCH_ACTIVE` (503)

## Sidecar Interaction Model

- Ktor calls sidecar over internal HTTP only (`AGENTKIT_SIDECAR_URL`)
- Shared secret header: `x-sidecar-secret`
- Sidecar returns unsigned calldata and quote/build artifacts; custody remains external

## Error Contract Mapping

Frontend and backend remain aligned through machine codes such as:

- `MISSING_IDEMPOTENCY_KEY`
- `IDEMPOTENCY_CONFLICT`
- `KILL_SWITCH_ACTIVE`
- `AGENTKIT_UNAVAILABLE`
- `RATE_LIMIT_EXCEEDED`

Client domain error mapping must stay one-to-one with these machine codes.
