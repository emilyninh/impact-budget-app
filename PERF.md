# Performance

A quick load test of the dashboard read path — the endpoints a page render hits, served from
the Redis-cached monthly aggregate (invalidate-on-write, rebuild-from-Postgres on a cold key).

## Test

[`perf/load-test.js`](perf/load-test.js) with [k6](https://k6.io): log in once, then ramp to
**30 concurrent users** and hold, each doing a batched dashboard render — `GET`
`/dashboard/summary` + `/transactions` + `/categories` + `/trend` — with 1 s think time.

```bash
docker compose up -d          # stack running, demo data seeded
k6 run perf/load-test.js      # or: k6 run -e BASE_URL=https://your-host perf/load-test.js
```

Profile: ramp 0→30 VUs over 15 s, hold 30 VUs for 45 s, ramp down 10 s (~70 s total).

## Results

Run on the local Docker stack (Apple Silicon / Colima), 30 VUs:

| Metric | Value |
| --- | --- |
| Requests | **6,845** |
| Failed | **0 (0.00%)** |
| Checks passed | **100% (6,845/6,845)** |
| Throughput | ~97 req/s |
| Latency — median | **8.5 ms** |
| Latency — p90 | 21.4 ms |
| Latency — **p95** | **25.0 ms** |
| Latency — max | 74.1 ms |

Thresholds (`p95 < 500 ms`, `error rate < 1%`) both passed with wide margin.

## Reading it

- This is a **realistic-usage** test (30 concurrent users with think time), not a
  max-throughput stress test — the ~97 req/s reflects the paced load, not a ceiling.
- The point: the cached read path stays in **single-digit-to-low-double-digit milliseconds
  under concurrency with zero errors**. Reads never touch the write path or recompute from
  Postgres on the hot path — the aggregate is maintained event-driven and served from Redis,
  falling back to a Postgres rebuild only on a cold key.
- Reproducible against any environment via `BASE_URL` (e.g. a deployed instance).
