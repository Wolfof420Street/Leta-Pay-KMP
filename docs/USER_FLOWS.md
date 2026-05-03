# USER_FLOWS

Final v6 flow documentation for LetaPay architecture.

## 1) Auth Flow

### Step-by-step

1. User connects wallet in client (WalletConnect session established).
2. Client requests nonce from backend auth endpoint.
3. Backend returns nonce/challenge bound to wallet address and expiry.
4. User signs SIWE message in external wallet.
5. Client sends wallet address + signed message + nonce context to backend verify endpoint.
6. Backend verifies signature, nonce freshness, and replay status.
7. Backend issues JWT session tokens.
8. Backend mints Firebase custom token for the same wallet identity.
9. Client stores session + firebase token and initializes Firebase auth/chat channels.

### Guarantees

- Non-custodial signing only (private keys never touch LetaPay infra).
- Nonce replay protection.
- Unified identity between JWT and Firebase custom auth.

## 2) Transaction Flow

### Step-by-step

1. User enters command in chat (`send`, `swap`, `stake`).
2. Parser performs AI/intent parse and emits structured intent.
3. Backend/agent planning phase builds execution plan and requests sidecar gas support as needed.
4. Backend calls sidecar build endpoint (transfer/swap/stake) for unsigned calldata.
5. Sidecar (AgentKit + CDP) returns unsigned calldata payload.
6. Backend returns preview/build response to client including normalized unsigned tx structure.
7. Client passes unsigned tx to WalletConnect signing flow.
8. Wallet returns signed transaction to client.
9. Client calls backend broadcast/send endpoint with signed transaction and idempotency key.
10. Backend records tx, broadcasts/upstreams status, and persists transaction state.
11. Client subscribes to summary/stream updates and renders final execution summary.

### Failure & Safety Paths

- `AGENTKIT_UNAVAILABLE`: client disables AI command actions and shows fallback messaging.
- Kill switch: backend blocks value-moving operations with service-unavailable machine code.
- Idempotency conflicts: deterministic replay or conflict rejection.
- Validation/screening failures: machine-coded errors and safe abort.

## 3) Data Contract Notes

- Build responses now include optional `unsignedTx` with calldata/gas fields.
- Error envelopes are machine-readable and map cleanly into client `AppError` variants.
- WalletConnect signer path consumes sidecar-backed unsigned calldata without client-side shape mutation.
