# Environment Variables

This table documents runtime and build-time variables used by LetaPay backend, sidecar, and frontend toolchains.

## Backend (`backend-ktor`)

| Variable Name | Required | Description | Safe Dummy Value (Local Dev) |
|---|---|---|---|
| `PORT` | Optional | HTTP bind port for Ktor. | `8080` |
| `OPENAI_API_KEY` | Required (prod) | OpenAI key used by Koog AI routes/models. | `sk-dev-placeholder` |
| `SESSION_SECRET` | Required (prod) | JWT signing secret for session tokens. | `dev-jwt-secret-change-me` |
| `REFRESH_TOKEN_PEPPER` | Required (prod) | Pepper for refresh token family security. | `dev-refresh-pepper-change-me` |
| `JWT_ISSUER` | Optional | JWT issuer override. | `leta-pay-backend` |
| `JWT_AUDIENCE` | Optional | JWT audience override. | `leta-pay-clients` |
| `AI_PARSE_MODEL` | Optional | Model override for parse route. | `gpt-4o-mini` |
| `AI_PLAN_MODEL` | Optional | Model override for plan route. | `gpt-4o` |
| `AI_CHAT_MODEL` | Optional | Model override for chat route. | `gpt-4o-mini` |
| `DATABASE_URL` | Required | JDBC URL used by Exposed/Hikari. | `jdbc:postgresql://postgres:5432/letapay` |
| `DATABASE_DRIVER` | Optional | JDBC driver class. | `org.postgresql.Driver` |
| `DATABASE_USER` | Optional | Database username. | `letapay` |
| `DATABASE_PASSWORD` | Optional | Database password. | `letapay` |
| `DB_MAX_POOL_SIZE` | Optional | Hikari max pool size. | `10` |
| `AGENTKIT_SIDECAR_URL` | Required | Internal URL to AgentKit sidecar. | `http://agentkit-sidecar:3100` |
| `SIDECAR_SECRET` | Required | Shared secret sent as `x-sidecar-secret`. | `dev-sidecar-secret` |
| `COINBASE_API_KEY` | Required (prod) | Coinbase CDP API key for backend Coinbase client calls. | `cb-dev-key` |
| `COINBASE_RISK_KEY` | Required (prod) | Coinbase risk/screening API key. | `risk-dev-key` |
| `FIREBASE_SA_JSON` | Required (prod) | Firebase Admin service account JSON payload. | `{}` |
| `FIREBASE_SA_PATH` | Optional | File path alternative to `FIREBASE_SA_JSON`. | `/tmp/firebase-sa.json` |
| `KILL_SWITCH_VALUE_MOVES` | Optional | When `true`, blocks value-moving routes. | `false` |
| `BASE_STAKING_ENABLED` | Optional | Enables/disables Base chain staking opportunities. | `false` |

## Sidecar (`agentkit-sidecar`)

| Variable Name | Required | Description | Safe Dummy Value (Local Dev) |
|---|---|---|---|
| `PORT` | Optional | Sidecar HTTP port. | `3100` |
| `SIDECAR_SECRET` | Required | Secret validated by internal-only middleware. | `dev-sidecar-secret` |
| `CDP_API_KEY_ID` | Required (prod) | Coinbase CDP API key id for AgentKit wallet provider. | `cdp-key-id-placeholder` |
| `CDP_API_KEY_SECRET` | Required (prod) | Coinbase CDP API key secret for AgentKit wallet provider. | `cdp-key-secret-placeholder` |
| `CDP_WALLET_SECRET` | Required (prod) | Wallet secret for CDP EVM wallet provider. | `cdp-wallet-secret-placeholder` |
| `BASE_RPC_URL` | Optional | Base RPC endpoint for gas estimation route. | `https://mainnet.base.org` |
| `ETHEREUM_RPC_URL` | Optional | Ethereum RPC endpoint for gas estimation route. | `https://ethereum.publicnode.com` |
| `POLYGON_RPC_URL` | Optional | Polygon RPC endpoint for gas estimation route. | `https://polygon-rpc.com` |

## Frontend / Build Tooling (KMP)

| Variable Name | Required | Description | Safe Dummy Value (Local Dev) |
|---|---|---|---|
| `VERSION` | Optional | Android `versionName` override. | `0.0.0-dev` |
| `VERSION_CODE` | Optional | Android `versionCode` override. | `1` |
| `KEYSTORE_PATH` | Optional | Android signing keystore path for release builds. | `../keystores/release_keystore.keystore` |
| `KEYSTORE_PASSWORD` | Optional | Android release keystore password. | `android` |
| `KEYSTORE_ALIAS` | Optional | Android release key alias. | `androiddebugkey` |
| `KEYSTORE_ALIAS_PASSWORD` | Optional | Android release key password. | `android` |
| `ORG_GRADLE_PROJECT_letapay.enableWasm` | Optional | Enables WASM target when set to true. | `false` |

## Docker Compose Service Variables

### `postgres`

- `POSTGRES_DB` (default in compose: `letapay`)
- `POSTGRES_USER` (default in compose: `letapay`)
- `POSTGRES_PASSWORD` (default in compose: `letapay`)

### `redis`

- no mandatory env vars in current compose file

## Notes

- Backend startup currently marks runtime as misconfigured when core secrets are absent (`OPENAI_API_KEY`, `SESSION_SECRET`, `REFRESH_TOKEN_PEPPER`, `DATABASE_URL`, `COINBASE_API_KEY`, `COINBASE_RISK_KEY`).
- Sidecar endpoints reject calls when `x-sidecar-secret` does not match `SIDECAR_SECRET`.
- Frontend app runtime network values are currently mostly provided by static app config per environment in code; env vars above are build-pipeline focused.
