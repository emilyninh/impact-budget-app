// k6 load test for the dashboard read path (the Redis-cached aggregate endpoints a page load
// hits). Ramps to 30 concurrent users; each iteration fetches summary + transactions +
// categories + trend in a batch, like one dashboard render, with 1s think time.
//
//   docker compose up -d           # stack running, demo data seeded
//   k6 run perf/load-test.js
//   k6 run -e BASE_URL=https://your-host perf/load-test.js
import http from "k6/http";
import { check, sleep } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    dashboard: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 30 }, // ramp up
        { duration: "45s", target: 30 }, // hold
        { duration: "10s", target: 0 }, // ramp down
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"], // <1% errors
    http_req_duration: ["p(95)<500"], // 95th percentile under 500ms
  },
};

export function setup() {
  const res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: "demo@impactbudget.app", password: "demopass123" }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(res, { "login ok": (r) => r.status === 200 });
  return { token: res.json("token") };
}

export default function (data) {
  const params = { headers: { Authorization: `Bearer ${data.token}` } };
  const responses = http.batch([
    ["GET", `${BASE}/api/v1/dashboard/summary`, null, params],
    ["GET", `${BASE}/api/v1/dashboard/transactions`, null, params],
    ["GET", `${BASE}/api/v1/dashboard/categories`, null, params],
    ["GET", `${BASE}/api/v1/dashboard/trend?months=6`, null, params],
  ]);
  responses.forEach((r) => check(r, { "status 200": (x) => x.status === 200 }));
  sleep(1);
}
