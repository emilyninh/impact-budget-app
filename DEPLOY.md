# Deploying Impact Budget to Fly.io

The app is one deployable (Spring Boot serving the bundled React SPA). It needs three private
sibling services on Fly's network: **Postgres**, **Redpanda** (Kafka API), and **Redis**.

> These steps require an authenticated `flyctl`. Run them yourself — they can't be automated
> for you. Everything the app needs is already in the repo (`Dockerfile`, `fly.toml`,
> `deploy/*.toml`); this is just the orchestration.

## 0. Prerequisites

```bash
brew install flyctl        # or: curl -L https://fly.io/install.sh | sh
fly auth login
```

Pick a unique app name (examples below use `impact-budget`). If you change it, update the
`.internal` hostnames in `fly.toml` to match.

## 1. Redpanda (Kafka)

```bash
fly launch --copy-config --config deploy/fly.redpanda.toml --no-deploy
fly volumes create redpanda_data --app impact-budget-redpanda --region sea --size 1
fly deploy --config deploy/fly.redpanda.toml
```

## 2. Redis

```bash
fly launch --copy-config --config deploy/fly.redis.toml --no-deploy
fly deploy --config deploy/fly.redis.toml
```

## 3. Postgres

```bash
fly postgres create --name impact-budget-db --region sea --initial-cluster-size 1 \
  --vm-size shared-cpu-1x --volume-size 1
```
Note the connection details it prints. The app uses a JDBC URL, so set it explicitly in step 5
(don't rely on `fly postgres attach`, which sets a non-JDBC `DATABASE_URL`).

## 4. Create the app (no deploy yet)

```bash
fly launch --copy-config --config fly.toml --no-deploy
```

## 5. Secrets

```bash
fly secrets set \
  DB_URL="jdbc:postgresql://impact-budget-db.internal:5432/impact_budget" \
  DB_USER="postgres" \
  DB_PASSWORD="<from step 3>" \
  JWT_SECRET="$(openssl rand -base64 48)" \
  --app impact-budget
```
(Optional real integrations: `ANTHROPIC_API_KEY`, `PLAID_CLIENT_ID`, `PLAID_SECRET`. Without
them the app uses the neutral scorer + free Open Food Facts / Wikidata enrichment, and the
seeded demo account.)

## 6. Deploy

```bash
fly deploy --config fly.toml
fly open        # opens the live URL
```

First boot runs Flyway migrations and seeds the demo account
(`demo@impactbudget.app` / `demopass123`). Watch it come up with `fly logs`.

## Verify

- `https://impact-budget.fly.dev/` → the dashboard (log in with the demo account).
- `https://impact-budget.fly.dev/actuator/health` → `{"status":"UP"}`.
- `https://impact-budget.fly.dev/swagger-ui/index.html` → API docs.

## CI auto-deploy (optional)

The GitHub Actions workflow deploys on green `main` if a `FLY_API_TOKEN` secret is present:

```bash
fly tokens create deploy -x 999999h        # copy the token
# GitHub → repo Settings → Secrets and variables → Actions → new secret FLY_API_TOKEN
```
Without the secret, the deploy job is skipped and CI just builds + tests + pushes the image.

## Troubleshooting

- **App unhealthy on boot** → `fly logs`. Usually a bad `DB_URL` or the DB/Redpanda app not
  reachable yet. Confirm the `.internal` names match your actual app names.
- **OOM / restarts** → bump `memory_mb` to `2048` in `fly.toml` and redeploy.
- **Kafka client can't connect** → the broker advertises `impact-budget-redpanda.internal:9092`;
  make sure that matches the Redpanda app name and that its machine is running
  (`fly status --app impact-budget-redpanda`). This private-network wiring is the most likely
  thing to need a tweak.
- **Managed-Kafka alternative** → instead of self-hosting Redpanda you can point
  `KAFKA_BOOTSTRAP_SERVERS` at a Redpanda Cloud serverless cluster; that requires adding
  `SPRING_KAFKA_SECURITY_PROTOCOL=SASL_SSL` and the SASL secrets (no code change — the Kafka
  factories read `spring.kafka.*`).
```
