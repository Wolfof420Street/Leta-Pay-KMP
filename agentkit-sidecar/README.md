# agentkit-sidecar

Internal Node.js sidecar that integrates AgentKit/Coinbase CDP and returns unsigned transaction calldata.

## Responsibilities

- Build unsigned transfer/swap/stake calldata through AgentKit providers.
- Provide gas-estimation support endpoints used by backend planning.
- Keep execution internal-only: no direct client access.

## Coinbase CDP / AgentKit Integration

- AgentKit instance is built per authenticated wallet context.
- Action providers map business operations (`transfer`, `swap`, `stake`) into unsigned calldata payloads.
- Responses are normalized to backend-consumable shape:

```json
{
  "success": true,
  "calldata": {
    "to": "0x...",
    "data": "0x...",
    "value": "0x...",
    "gasLimit": "0x...",
    "maxFeePerGas": "0x...",
    "maxPriorityFeePerGas": "0x...",
    "chainId": 1
  }
}
```

## Security Model (Internal-Only)

- Sidecar endpoints are protected by internal middleware and must not be internet-exposed.
- Backend is the only supported caller.
- Sensitive CDP credentials live in sidecar env only.
- Sidecar never signs or broadcasts user transactions.

## Local Run

```bash
npm install
npm run dev
```

Or via project docker-compose with backend.
