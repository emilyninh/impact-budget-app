---
name: run-app-locally
description: Start the Impact Budget app locally end-to-end (Docker stack + React frontend) and verify it's healthy. Use when the user wants to run, start, boot, or bring up the app locally, or asks "how do I run this".
---

# Run Impact Budget locally

Brings up the full stack (Spring Boot app + Postgres + Redpanda/Kafka + Redis + Prometheus +
Grafana) via Docker Compose, plus the React frontend. Written for **macOS + Colima** (this
machine's setup). With Docker Desktop instead of Colima, skip the Colima steps.

## 0. One-time prerequisites (already installed on this machine)

```bash
brew install colima docker docker-compose node   # engine, CLI, compose plugin, Node
# link the compose plugin so `docker compose` works (one time):
mkdir -p ~/.docker/cli-plugins && \
  ln -sfn "$(brew --prefix)/opt/docker-compose/bin/docker-compose" ~/.docker/cli-plugins/docker-compose
```

Optional, for real (non-fallback) impact scores — free & local:
```bash
brew install ollama && ollama pull llama3.1     # then run `ollama serve` (see step 4)
```

## 1. Start the Docker engine (Colima)

```bash
colima status || colima start --cpu 4 --memory 8 --disk 60
```
Colima does **not** auto-start after a reboot — run `colima start` again if `docker ps` fails
with "cannot connect to the Docker daemon".

## 2. Bring up the backend stack

From the repo root (`/Users/emilyninh/workspace/personal-finance`):

```bash
docker compose up --build -d
```

First run builds the app image with Maven inside the container (a few minutes; cached after).
The `Docker Compose requires buildx plugin` warning is harmless (legacy builder is used).

## 3. Wait for the app to be healthy

```bash
until [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health)" = 200 ]; do
  echo "waiting for app..."; sleep 3; done && echo "app UP"
docker compose ps            # all services Up; postgres/redis/redpanda healthy
```

Sanity-check the API (empty until transactions are ingested — see step 6):
```bash
curl -s http://localhost:8080/api/dashboard/summary
```

## 4. (Optional) Start Ollama for real scoring

The default scorer is a local Ollama model. Without it, scoring uses a neutral fallback
(the app still runs). To enable it, in a separate terminal:
```bash
ollama serve                 # serves on :11434; the app reaches it via host.docker.internal
```
The container already points at `host.docker.internal:11434` (see `docker-compose.yml`).
To use Claude instead: set `SCORING_PROVIDER=claude` and `ANTHROPIC_API_KEY` in `.env`.

## 5. Start the frontend (React UI)

```bash
cd frontend
npm install                  # first time only
npm run dev                  # serves http://localhost:5173 (proxies /api -> :8080)
```

## URLs

| Service          | URL                                   |
| ---------------- | ------------------------------------- |
| Frontend (UI)    | http://localhost:5173                 |
| App / API        | http://localhost:8080                 |
| Health           | http://localhost:8080/actuator/health |
| Prometheus       | http://localhost:9090                 |
| Grafana          | http://localhost:3000 (admin/admin)   |
| Redpanda console | http://localhost:8085                 |

## 6. Getting data in (the dashboard is empty until then)

Transactions enter only via Plaid. There is no built-in demo seed. Options:
- **Plaid Sandbox:** put `PLAID_CLIENT_ID` / `PLAID_SECRET` (free sandbox keys from
  dashboard.plaid.com) in `.env`, `docker compose up -d` to reload, then drive the Link flow:
  `POST /api/plaid/link-token` → link in Plaid Link → `POST /api/plaid/exchange` → the app
  syncs transactions, which flow through Kafka → scoring → budget → dashboard.
- If asked to demo without Plaid, the honest path is to add a small dev-only seeder that
  inserts a few `bank_transaction` rows and publishes `TransactionIngested` events — offer
  this rather than hand-inserting rows (impact_score has an FK to bank_transaction).

## Stop / clean up

```bash
# Ctrl-C the `npm run dev` and `ollama serve` terminals, then:
docker compose down          # stop containers (keep the Postgres volume)
docker compose down -v       # also delete data (fresh start next time)
colima stop                  # optional: stop the VM to free resources
```

## Troubleshooting

- **`docker` "cannot connect to the daemon"** → `colima start`.
- **Build fails `no matching manifest for linux/arm64`** → the runtime base must be multi-arch;
  the Dockerfile uses `eclipse-temurin:17-jre` (not `-jre-alpine`, which has no arm64). Already fixed.
- **App unhealthy / restarting** → `docker compose logs app | tail -50`. Common causes: a
  dependency container not healthy yet (it retries), or a bad `.env` value.
- **Port already in use (8080/5432/9092/6379/3000/9090/5173)** → stop the conflicting process
  or change the host port mapping in `docker-compose.yml`.
- **`mvn verify` container tests** (separate from running the app) need Testcontainers to find
  Colima's socket — create `~/.testcontainers.properties` with
  `docker.host=unix:///Users/<you>/.colima/docker.sock`. Not needed just to run the app.
