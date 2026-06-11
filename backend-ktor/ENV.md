# Leta Pay Backend Environment

## Required

The backend validates these at startup. If any are missing, the server still boots, but
`/health` reports `status = "misconfigured"`.

- `OPENAI_API_KEY`: OpenAI API key for Koog-backed fallback parsing and planning.
- `JWT_PRIVATE_KEY`: Optional in dev, required for RS256 production mode.
- `JWT_PUBLIC_KEY`: Optional in dev, required for RS256 production mode.
- `SESSION_SECRET`: HMAC secret used to sign and verify session JWTs.
- `REFRESH_TOKEN_PEPPER`: Pepper appended before Argon2id hashing refresh tokens.
- `DATABASE_URL`: JDBC URL for the backend database.
- `FIREBASE_SA_JSON`: Firebase Admin service account JSON payload.
- `COINBASE_API_KEY`: Coinbase/CDP API key for pricing, swaps, and onchain lookups.
- `COINBASE_RISK_KEY`: Coinbase Risk API key for address screening.
- `APP_DOMAIN`: SIWE domain used for backend verification.
- `APP_ORIGIN`: SIWE origin used for backend verification.
- `REDIS_URL`: Redis connection string for backend rate limiting and shared caches.
- `SIDECAR_SECRET`: Shared secret for backend-to-sidecar internal-auth calls.
- `MAX_TRANSACTION_VALUE_ETH`: Maximum permitted transaction value for value-moving builds.

## Optional

- `ENVIRONMENT`: Alias for `ENV` accepted by the backend env loader.
- `PORT`: HTTP port to bind. Default: `8080`.
- `AI_PARSE_MODEL`: Model used for AI parse fallback. Default: `gpt-4o-mini`.
- `AI_PLAN_MODEL`: Model used for planning fallback. Default: `gpt-4o`.
- `AI_CHAT_MODEL`: Model used for streaming chat fallback. Default: `gpt-4o-mini`.
- `KILL_SWITCH_VALUE_MOVES`: When `true`, blocks value-moving routes. Default: `false`.
- `DB_MIN_POOL_SIZE`: Minimum Hikari pool size. Default: `1`.
- `DB_MAX_POOL_SIZE`: Maximum Hikari pool size. Default: `10`.
- `DB_CONNECTION_TIMEOUT_MS`: Hikari connection timeout in milliseconds. Default: `30000`.
- `AGENTKIT_SIDECAR_URL`: Optional explicit backend-to-sidecar URL override.
- `DATABASE_DRIVER`: Optional JDBC driver override.
- `DATABASE_USER`: Optional database username.
- `DATABASE_PASSWORD`: Optional database password.
- `JWT_ISSUER`: Optional JWT issuer override. Default: `leta-pay-backend`.
- `JWT_AUDIENCE`: Optional JWT audience override. Default: `leta-pay-clients`.
- `FIREBASE_SA_PATH`: Optional file path alternative to `FIREBASE_SA_JSON`.

## Local Smoke Test

Example H2 URL:

```text
jdbc:h2:mem:letapay;DB_CLOSE_DELAY=-1
```
