# API Documentation

This document describes the Ktor backend API for Leta Pay.

## Base URL
- Development: `http://localhost:8080`
- Production: `https://api.letapay.org`

## Authentication

Most endpoints require a JWT token in the `Authorization` header.
`Authorization: Bearer <token>`

### 1. Request SIWE Nonce
`POST /auth/request-nonce`
- **Body**: `{ "walletAddress": "0x..." }`
- **Response**: `{ "nonce": "...", "expiresAt": "..." }`
- **Auth**: None

### 2. Verify SIWE Signature
`POST /auth/verify-signature`
- **Body**: `{ "walletAddress": "0x...", "signature": "0x...", "message": "..." }`
- **Response**: `{ "accessToken": "...", "refreshToken": "..." }`
- **Auth**: None

### 3. Refresh Token
`POST /auth/refresh-token`
- **Body**: `{ "refreshToken": "..." }`
- **Response**: `{ "accessToken": "...", "refreshToken": "..." }`
- **Auth**: None

---

## DeFi Operations

### 1. Get Price Quote
`GET /prices/{chain}/{asset}`
- **Parameters**: `chain` (e.g., `8453`), `asset` (e.g., `ETH`)
- **Response**: `{ "price": "...", "timestamp": "..." }`
- **Auth**: None

### 2. Get Swap Quote
`POST /swap/quote`
- **Body**: `{ "fromToken": "...", "toToken": "...", "fromAmount": "...", "slippageBps": 50 }`
- **Response**: `{ "quoteId": "...", "fromAmount": "...", "toAmount": "...", "rate": "...", "estimatedFeeUsd": "..." }`
- **Auth**: Required

### 3. Execute Swap
`POST /swap/execute`
- **Headers**: `Idempotency-Key: <uuid>`
- **Body**: `{ "quoteId": "...", "idempotencyKey": "..." }`
- **Response**: `{ "unsignedTx": { ... }, "expiresAt": "..." }`
- **Auth**: Required

### 4. Get Yield Opportunities
`GET /yield/opportunities`
- **Query**: `chain` (optional)
- **Response**: `[ { "opportunityId": "...", "asset": "...", "apy": "...", "chain": 8453 }, ... ]`
- **Auth**: Required

### 5. Stake Asset
`POST /yield/stake`
- **Headers**: `Idempotency-Key: <uuid>`
- **Body**: `{ "opportunityId": "...", "amount": "...", "idempotencyKey": "..." }`
- **Response**: `{ "positionId": "...", "unsignedTx": { ... } }`
- **Auth**: Required

---

## AI Assistant

### 1. Chat Stream (SSE)
`GET /ai/chat-stream`
- **Query**: `event` (optional), `txHash` (optional)
- **Stream**: `data: <chunk>`
- **Auth**: Required

### 2. Parse Intent
`POST /ai/parse`
- **Body**: `{ "message": "..." }`
- **Response**: `{ "intent": "...", "entities": { ... } }`
- **Auth**: Required

### 3. Generate Plan
`POST /ai/plan`
- **Body**: `{ "message": "..." }`
- **Response**: `{ "planId": "...", "steps": [ ... ] }`
- **Auth**: Required

---

## Common Headers
- `X-Device-Fingerprint`: Used for session binding.
- `Idempotency-Key`: Required for state-changing operations (Swap, Stake).

## Error Codes
- `400 Bad Request`: Validation failure.
- `401 Unauthorized`: Missing or invalid token.
- `403 Forbidden`: Rate limit exceeded or kill switch active.
- `408 Request Timeout`: Upstream service timeout.
- `500 Internal Server Error`: Backend error.
