# Leta Pay VPS Deployment (Ubuntu + Docker Compose)

This runbook deploys Leta Pay in a self-hosted VPS setup with these containers:

- `postgres`
- `redis`
- `ktor-backend`
- `agentkit-sidecar`
- `web-frontend` (Nginx serving `:cmp-web:jsBrowserDistribution` output)

## 0. Server Prep (Fresh Ubuntu)

Before starting, prepare your fresh Ubuntu 22.04/24.04 server with these commands:

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essential tools
sudo apt install -y curl git unzip zip openjdk-21-jdk

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Setup project directory
sudo mkdir -p /var/www/letapay
sudo chown $USER:$USER /var/www/letapay
```

## 1. Prerequisites

- Ubuntu 22.04 or 24.04 VPS
- DNS records already pointed to your VPS
- Docker Engine + Docker Compose plugin installed
- Git installed
- Ports open in firewall:
  - `80/tcp`
  - `443/tcp`

Install dependencies:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg git

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
newgrp docker
```

## 2. Clone Repository

```bash
git clone <YOUR_REPO_URL> leta-pay
cd leta-pay
```

## 3. Create Production `.env`

Create `.env` in repository root:

```dotenv
# Compose service ports (bound to loopback)
BACKEND_PORT=8080
WEB_PORT=8081

# Database
POSTGRES_DB=letapay
POSTGRES_USER=letapay
POSTGRES_PASSWORD=CHANGE_ME_STRONG_DB_PASSWORD
DB_MAX_POOL_SIZE=10

# Internal service authentication
SIDECAR_SECRET=CHANGE_ME_LONG_RANDOM_SECRET

# Ktor backend required secrets
OPENAI_API_KEY=sk-...
SESSION_SECRET=CHANGE_ME_LONG_RANDOM_JWT_SECRET
REFRESH_TOKEN_PEPPER=CHANGE_ME_LONG_RANDOM_PEPPER
JWT_ISSUER=leta-pay-backend
JWT_AUDIENCE=leta-pay-clients
FIREBASE_SA_JSON={"type":"service_account",...}
COINBASE_API_KEY=...
COINBASE_RISK_KEY=...
KILL_SWITCH_VALUE_MOVES=false

# AgentKit sidecar secrets
CDP_API_KEY_ID=...
CDP_API_KEY_SECRET=...
CDP_WALLET_SECRET=...
BASE_RPC_URL=https://mainnet.base.org
ETHEREUM_RPC_URL=https://ethereum.publicnode.com
POLYGON_RPC_URL=https://polygon-rpc.com
```

Notes:
- `SIDECAR_SECRET` must match for both backend and sidecar.
- `FIREBASE_SA_JSON` is a single-line JSON string.
- Use strong random values for `POSTGRES_PASSWORD`, `SESSION_SECRET`, and `REFRESH_TOKEN_PEPPER`.

## 4. Build KMP Web Distribution

Build static web assets that Nginx will serve:

```bash
./gradlew --no-daemon --no-configuration-cache :cmp-web:jsBrowserDistribution
```

Expected output directory:

- `cmp-web/build/dist/js/productionExecutable/`

## 5. Build and Start Production Stack

```bash
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Check status:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f ktor-backend
docker compose -f docker-compose.prod.yml logs -f agentkit-sidecar
```

Health checks:

```bash
curl -fsS http://127.0.0.1:8080/health
curl -fsS http://127.0.0.1:8081/
```

## 6. Reverse Proxy Option A: Caddy (Recommended)

Install Caddy:

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install -y caddy
```

Example `/etc/caddy/Caddyfile`:

```caddy
letapay.example.com {
  reverse_proxy 127.0.0.1:8081
}

api.letapay.example.com {
  reverse_proxy 127.0.0.1:8080
}
```

Apply config:

```bash
sudo systemctl reload caddy
```

## 7. Reverse Proxy Option B: Nginx + Certbot

Install:

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

Example `/etc/nginx/sites-available/letapay`:

```nginx
server {
    listen 80;
    server_name letapay.example.com;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name api.letapay.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable and secure with TLS:

```bash
sudo ln -s /etc/nginx/sites-available/letapay /etc/nginx/sites-enabled/letapay
sudo nginx -t
sudo systemctl reload nginx

sudo certbot --nginx -d letapay.example.com -d api.letapay.example.com
```

## 8. Upgrade Procedure

```bash
git pull --ff-only
./gradlew --no-daemon --no-configuration-cache :cmp-web:jsBrowserDistribution
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

## 9. Operations Notes

- Sidecar is internal-only; do not expose port `3100` publicly.
- Use `KILL_SWITCH_VALUE_MOVES=true` for emergency transaction stop.
- Backup `postgres_data` volume regularly.
- Re-run web distribution build after any KMP web UI change.
