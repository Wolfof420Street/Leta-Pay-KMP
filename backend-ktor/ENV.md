# Leta Pay Backend Environment

## Required

The backend validates these at startup. If any are missing, the server still boots, but
`/health` reports `status = "misconfigured"`.

- `OPENAI_API_KEY`: OpenAI API key for Koog-backed fallback parsing and planning.
- `SESSION_SECRET`: HMAC secret used to sign and verify session JWTs.
- `REFRESH_TOKEN_PEPPER`: Pepper appended before Argon2id hashing refresh tokens.
- `DATABASE_URL`: JDBC URL for the backend database.
- `FIREBASE_SA_JSON`: Firebase Admin service account JSON payload.
- `COINBASE_API_KEY`: Coinbase/CDP API key for pricing, swaps, and onchain lookups.
- `COINBASE_RISK_KEY`: Coinbase Risk API key for address screening.

## Optional

- `PORT`: HTTP port to bind. Default: `8080`.
- `AI_PARSE_MODEL`: Model used for AI parse fallback. Default: `gpt-4o-mini`.
- `AI_PLAN_MODEL`: Model used for planning fallback. Default: `gpt-4o`.
- `AI_CHAT_MODEL`: Model used for streaming chat fallback. Default: `gpt-4o-mini`.
- `KILL_SWITCH_VALUE_MOVES`: When `true`, blocks value-moving routes. Default: `false`.
- `DB_MAX_POOL_SIZE`: Maximum Hikari pool size. Default: `10`.
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
