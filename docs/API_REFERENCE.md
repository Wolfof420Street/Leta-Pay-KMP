# API Reference

All responses are JSON. Protected endpoints require backend session auth.

## Error Envelope

Backend domain and validation errors are normalized as:

```json
{
  "code": "MACHINE_CODE",
  "message": "Human readable message",
  "retryAfter": 30
}
```

`retryAfter` is optional and appears for throttling/backoff scenarios.

## POST /transactions/build

Builds an unsigned transfer transaction.

### Headers

- `Authorization: Bearer <session_token>`
- `Idempotency-Key: <uuid>`
- `X-Chain-Id: <long>` (optional, defaults to `1`)

### Request Body

```json
{
  "to": "0x1111111254EEB25477B68fb85Ed929f73A960582",
  "amount": "0.10",
  "asset": "ETH"
}
```

### Success Response (200)

```json
{
  "status": "prepared",
  "preview": "Prepared unsigned transaction for 0x1111111254EEB25477B68fb85Ed929f73A960582.",
  "unsignedTx": {
    "to": "0x1111111254EEB25477B68fb85Ed929f73A960582",
    "data": "0x",
    "value": "0x0",
    "gasLimit": "0x493e0",
    "maxFeePerGas": "0x0",
    "maxPriorityFeePerGas": "0x0",
    "chainId": 1,
    "nonce": null
  }
}
```

### Typical Errors

- `KILL_SWITCH_ACTIVE` (503)
- `MISSING_IDEMPOTENCY_KEY` (400)
- `IDEMPOTENCY_CONFLICT` (409)
- `ADDRESS_REJECTED` (422)
- `RATE_LIMIT_EXCEEDED` (429)
- `AGENTKIT_UNAVAILABLE` (503)

## POST /transactions/send

Submits a signed transaction for broadcast/recording.

### Headers

- `Authorization: Bearer <session_token>`
- `Idempotency-Key: <uuid>`
- `X-To-Address: <0x...>`
- `X-Asset: <asset_symbol>`
- `X-Amount: <decimal_string>`
- `X-Chain-Id: <long>`

### Request Body

```json
{
  "signedTx": "0x02f8..."
}
```

### Success Response (200)

```json
{
  "txHash": "0x4a5f0bc2c76f357fbb5dce7f9f13be4e2f8f2f6d6283931b9e8f8b3f90ab12cd",
  "status": "submitted"
}
```

### Typical Errors

- `KILL_SWITCH_ACTIVE` (503)
- `MISSING_IDEMPOTENCY_KEY` (400)
- `IDEMPOTENCY_CONFLICT` (409)
- `RATE_LIMIT_EXCEEDED` (429)

## POST /swap/quote

Creates swap quote snapshot.

### Headers

- `Authorization: Bearer <session_token>`

### Request Body

```json
{
  "fromAsset": "ETH",
  "toAsset": "USDC",
  "amount": "0.25",
  "chain": 8453,
  "slippageBps": 50
}
```

### Success Response (200)

```json
{
  "quoteId": "quote-1718999999999",
  "fromAmount": "0.25",
  "toAmount": "923.45",
  "rate": "3693.80",
  "priceImpactBps": 25,
  "estimatedFeeUsd": "0.12",
  "expiresAt": 1719000059999
}
```

### Typical Errors

- `KILL_SWITCH_ACTIVE` (503)
- `BAD_REQUEST` (400) for unsupported chain/asset/slippage
- `SLIPPAGE_EXCEEDED` (422)
- `UPSTREAM_TIMEOUT` (504)
- `AGENTKIT_UNAVAILABLE` (503)

## POST /yield/stake

Builds unsigned stake tx and creates staking position.

### Headers

- `Authorization: Bearer <session_token>`
- `Idempotency-Key: <uuid>` (must match body)

### Request Body

```json
{
  "opportunityId": "lido-eth-1",
  "amount": "0.50",
  "idempotencyKey": "c988bb8d-4d5f-4f12-b907-4d40cd2b4f30"
}
```

### Success Response (200)

```json
{
  "positionId": "pos_01J9YQ6Q3Q7J2AA6ZG6W8Q8F4W",
  "opportunityId": "lido-eth-1",
  "walletAddress": "0x0f9a4cf2c8bc4a42f7b560544e10fcd77d88d6a9",
  "stakedAmount": "0.50",
  "currentValue": "0.50",
  "currentValueUsd": "0.00",
  "accruedRewards": "0",
  "status": "Active",
  "chain": 1,
  "createdAt": 1719000000123,
  "unsignedTx": {
    "to": "0xae7ab96520DE3A18E5e111B5EaAb095312D7fE84",
    "data": "0x",
    "value": "0x0",
    "gasLimit": "0x493e0",
    "maxFeePerGas": "0x0",
    "maxPriorityFeePerGas": "0x0",
    "chainId": 1,
    "nonce": null
  }
}
```

### Typical Errors

- `KILL_SWITCH_ACTIVE` (503)
- `MISSING_IDEMPOTENCY_KEY` (400)
- `BAD_REQUEST` (400)
- `OPPORTUNITY_NOT_FOUND` (400)
- `OPPORTUNITY_DISABLED` (400)
- `IDEMPOTENCY_CONFLICT` (409)
- `AGENTKIT_UNAVAILABLE` (503)

## Error Code Catalog (Core)

- `BAD_REQUEST`
- `VALIDATION_FAILED`
- `UNAUTHORIZED_SESSION`
- `RATE_LIMIT_EXCEEDED`
- `KILL_SWITCH_ACTIVE`
- `MISSING_IDEMPOTENCY_KEY`
- `IDEMPOTENCY_CONFLICT`
- `ADDRESS_REJECTED`
- `QUOTE_EXPIRED`
- `QUOTE_MISMATCH`
- `SLIPPAGE_EXCEEDED`
- `UPSTREAM_TIMEOUT`
- `AGENTKIT_UNAVAILABLE`
- `INTERNAL_ERROR`
