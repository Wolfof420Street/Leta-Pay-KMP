# Local Development

This runbook starts the full local stack and runs all supported client targets (Android, Desktop, Web).

## 1) Prerequisites

- JDK 17+ (JDK 21 recommended)
- Android Studio (for Android target)
- Node.js 22+ (for sidecar toolchain)
- Docker + Docker Compose
- Git + shell (`bash`/`zsh`)

## 2) Configure Environment

1. Create a root `.env` file (consumed by docker-compose).
2. Add at minimum:

```dotenv
SIDECAR_SECRET=dev-sidecar-secret
CDP_API_KEY_ID=cdp-key-id-placeholder
CDP_API_KEY_SECRET=cdp-key-secret-placeholder
CDP_WALLET_SECRET=cdp-wallet-secret-placeholder
BASE_RPC_URL=https://mainnet.base.org
ETHEREUM_RPC_URL=https://ethereum.publicnode.com
POLYGON_RPC_URL=https://polygon-rpc.com
```

3. Export backend secrets in your shell before starting backend outside compose, or inject through compose overrides:

```bash
export OPENAI_API_KEY=sk-dev-placeholder
export SESSION_SECRET=dev-jwt-secret-change-me
export REFRESH_TOKEN_PEPPER=dev-refresh-pepper-change-me
export COINBASE_API_KEY=cb-dev-key
export COINBASE_RISK_KEY=risk-dev-key
export FIREBASE_SA_JSON='{}'
```

## 3) Start Docker Compose Stack

```bash
docker-compose build
docker-compose up -d
```

Health checks/log tail:

```bash
docker-compose ps
docker-compose logs -f ktor-backend
docker-compose logs -f agentkit-sidecar
```

Stop stack:

```bash
docker-compose down
```

## 4) Run Client Targets Locally

### Android

```bash
./gradlew --no-daemon :cmp-android:installProdDebug --no-configuration-cache
# or compile gate
./gradlew --no-daemon :cmp-android:compileProdDebugKotlin --no-configuration-cache
```

Open Android Studio for emulator/device deployment if needed.

### Desktop

```bash
./gradlew --no-daemon :cmp-desktop:run --no-configuration-cache
# or compile gate
./gradlew --no-daemon :cmp-desktop:compileKotlinJvm --no-configuration-cache
```

### Web

```bash
./gradlew --no-daemon :cmp-web:jsBrowserDevelopmentRun --no-configuration-cache
# or compile gate
./gradlew --no-daemon :cmp-web:compileKotlinJs --no-configuration-cache
```

## 5) Recommended Validation Gates

Run these before pushing PRs:

```bash
./gradlew --no-daemon :cmp-android:compileProdDebugKotlin --no-configuration-cache
./gradlew --no-daemon :cmp-desktop:compileKotlinJvm --no-configuration-cache
./gradlew --no-daemon :cmp-web:compileKotlinJs --no-configuration-cache
./gradlew --no-daemon :backend-ktor:test --no-configuration-cache
```

## 6) Troubleshooting

- `403 Forbidden` from sidecar: verify `SIDECAR_SECRET` matches backend `SIDECAR_SECRET`.
- Backend `misconfigured` state: verify required backend env vars are present.
- Build signing failures (Android release): verify `KEYSTORE_*` variables.
- Sidecar AgentKit errors: verify `CDP_*` credentials and RPC URL reachability.
