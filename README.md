# Impact Budget

**A budgeting tool that categorizes spending by _impact_, not by type.**

Traditional budgeting apps tell you _how much_ you spent on "Groceries" or "Shopping."
Impact Budget tells you _where that money went_: what share of your discretionary
spending flowed to **local, independent businesses** versus multinational
conglomerates, and how **sustainable** your purchases were (natural fibers vs.
synthetic fast fashion, organic vs. conventional, B‑Corp vs. not). Then it lets you set
goals — not just "save more," but "shift 30% of my discretionary spending to local
businesses by Q4" — and tracks your progress over time.

It's a financial mirror for your values.

> **Status:** in active development. See [build progress](#build-progress) below.

---

## Why this project exists

This is a portfolio project built to demonstrate senior backend engineering: real
third‑party integration, event‑driven architecture, an LLM‑powered service, caching,
observability, and a real test suite — all wired into one runnable system.

The genuinely hard problem here isn't the plumbing; it's **where the impact scores come
from**. "Is `TST*SQ*LOCAL COFFEE 12345` a local independent business?" is a real data
problem, and it gets the most design attention (see [Categorization](#2-categorization--the-core-value)).

## Architecture

A **modular monolith** — one deployable Spring Boot app with clean module boundaries —
communicating over Kafka. Not microservices: the goal is a system that's impressive
_and_ finishable and runnable.

```
Plaid Sandbox ──webhook──▶ [ingestion] ──TransactionIngested──▶ transactions.ingested
                               │                                        │
                         Postgres (raw txns,                   ┌────────┴─────────┐
                          idempotent on                        ▼                  ▼
                          plaid txn_id)                 [categorization]     [budget]
                                                        Claude + curated     updates Redis
                                                        overrides            aggregates
                                                              │
                                                        TransactionScored ─▶ transactions.scored
                                                              │                  │
                                                              ▼                  ▼
                                                        Postgres            [budget] recompute
                                                        (impact_score)      impact %  (Redis)
                                                                                 │
React dashboard ◀── REST (Redis aggregates, Postgres fallback) ◀─────────────────┘
```

The headline event‑driven pattern is **one event, two consumers**:
`transactions.ingested` fans out to both the categorization engine and the budget cache.

### Modules

| Module          | Responsibility                                                            |
| --------------- | ------------------------------------------------------------------------- |
| `ingestion`     | Plaid client, webhook listener, idempotent transaction persistence        |
| `categorization`| Merchant normalization, Claude scoring, curated overrides, score cache    |
| `budget`        | Goals, discretionary‑spend classification, Redis aggregates               |
| `dashboard`     | REST API for the React frontend                                           |
| `common`        | Shared domain types and event schemas                                     |

## Tech stack

- **Backend:** Java 17, Spring Boot 3.3, Spring Modulith (verifiable boundaries)
- **Persistence:** PostgreSQL + Flyway migrations
- **Messaging:** Kafka (run locally as Redpanda)
- **Cache:** Redis
- **AI categorization:** Anthropic Claude (`claude-opus-4-8`) via structured outputs
- **Banking data:** Plaid (Sandbox)
- **Observability:** Actuator + Micrometer + Prometheus + Grafana
- **Frontend:** React + TypeScript (Vite) + Recharts
- **Testing:** JUnit 5, Mockito, Testcontainers (Postgres/Kafka/Redis), Vitest
- **CI:** GitHub Actions

## Running locally

Everything runs via Docker Compose — the app image builds with Maven inside the
container, so you don't need Maven or Node installed to run the stack.

```bash
cp .env.example .env         # add your ANTHROPIC_API_KEY and Plaid sandbox keys
docker compose up --build
```

| Service           | URL                              |
| ----------------- | -------------------------------- |
| App               | http://localhost:8080            |
| Health            | http://localhost:8080/actuator/health |
| Redpanda Console  | http://localhost:8085            |
| Prometheus        | http://localhost:9090            |
| Grafana           | http://localhost:3000 (admin/admin) |

### Local dev without Docker

The build **targets JDK 17** (matching CI). It also runs on newer JDKs — Byte Buddy is
pinned to a current version and `net.bytebuddy.experimental` is set for the test JVMs, so
Mockito works on JDK 24+ too. Requires Maven 3.9+. Start the infra containers and run the app:

```bash
docker compose up -d postgres redpanda redis
mvn spring-boot:run
```

Frontend dev server (proxies `/api` to the backend on :8080):

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

## Observability

Actuator exposes health and Prometheus metrics at `/actuator/health` and
`/actuator/prometheus`. Prometheus scrapes the app; Grafana (auto-provisioned datasource +
the **Impact Budget** dashboard) visualizes it at http://localhost:3000. Custom metrics:

| Metric | What it shows |
| --- | --- |
| `categorization_cache_total{result}` | merchant-score cache hit vs. miss (→ hit rate) |
| `categorization_scoring_total{source}` | scoring by `llm` vs. `fallback` |
| `categorization_claude_latency_seconds` | Claude round-trip latency (p95 on the dashboard) |
| `kafka_consumer_fetch_manager_records_lag` | consumer lag per client |
| `http_server_requests_seconds`, `jvm_memory_used_bytes` | standard HTTP/JVM |

## Testing

```bash
mvn verify        # unit tests + Testcontainers integration test (needs Docker for the IT)
```

The suite has three layers:

- **Unit** (Mockito) — sync idempotency, merchant normalization, curated overrides, the
  cache-hit path (proving no second LLM call), aggregate math, goal progress.
- **Architecture** — a Spring Modulith test statically verifies module boundaries (no
  cycles, no reaching into another module's internals).
- **Integration** (Testcontainers) — an end-to-end test publishes a `TransactionIngested`
  event and asserts it flows through the categorization and budget consumers against real
  Postgres/Kafka/Redis. Container-based tests skip cleanly when Docker isn't present, so
  `mvn verify` is green locally without Docker and fully exercised in CI.

## Build progress

- [x] **Step 1 — Skeleton & infra:** Spring Boot app, Postgres + Flyway, Docker Compose
      (Postgres/Redpanda/Redis/Prometheus/Grafana), Actuator health, Testcontainers
      smoke test, CI.
- [x] **Step 2 — Plaid ingestion:** link/exchange endpoints, cursor-based `/transactions/sync`,
      webhook listener, idempotent upsert on `plaid_transaction_id`, Resilience4j retry/backoff.
- [x] **Step 3 — Kafka events:** `TransactionIngested` published on new rows (keyed by user),
      consumer scaffold in categorization; JSON serializers use Spring's ObjectMapper.
- [x] **Step 4 — Categorization:** merchant normalization, `merchant_score` cache,
      seeded `curated_merchant` overrides, Claude scoring (keyless fallback), `impact_score`
      persistence, `TransactionScored` published.
- [x] **Step 5 — Budget & goals:** budget-owned `scored_transaction` projection from
      `TransactionScored`, spend-weighted monthly aggregates cached in Redis (invalidate on
      write, rebuild from Postgres on cold read), goal model + live progress tracking.
- [x] **Step 6 — Dashboard & UI:** read API (`/api/dashboard/*`, `/api/goals`) over the
      Redis-cached aggregate; React + TypeScript + Recharts frontend (impact summary,
      local-vs-sustainability trend, goal tracker with progress bars, transaction list).
- [x] **Step 7 — Observability & tests:** custom Micrometer metrics (cache hit rate, Claude
      latency, scoring source), provisioned Grafana dashboard, Spring Modulith boundary test,
      end-to-end Testcontainers integration test.

## The impact‑scoring design

Merchant strings from banks are messy (`TST*SQ*LOCAL COFFEE 12345`). Scoring them is a
three‑stage pipeline that keeps accuracy high and LLM cost low:

1. **Normalize & cache.** Strip processor prefixes (`TST*`, `SQ*`), store IDs, and
   trailing digits, then look up a `merchant_score` table. A hit means _no LLM call_ —
   most spending repeats the same merchants.
2. **Score on miss.** Call Claude (`claude-opus-4-8`) with the target JSON schema
   described in the system prompt; the response is parsed and validated with Jackson into
   a typed result (**Local score**, **Sustainability score**, material flags, confidence,
   rationale). If no API key is configured, or the call/parse fails, it falls back to a
   neutral heuristic so the pipeline never blocks — the app runs keyless.
3. **Apply curated overrides.** A seeded `curated_merchant` table (known national chains,
   the B‑Corp registry, known fast‑fashion/sustainable brands) is ground truth and
   _corrects_ the LLM. The curated table wins on conflict.

Each impact score records its `source` (`LLM`, `CURATED`, `FALLBACK`, or `CACHE`), so a
repeat merchant served from the cache is visibly distinct from a fresh LLM call — this is
honest about uncertainty (scores carry a confidence) while staying cheap and fast in
steady state.
