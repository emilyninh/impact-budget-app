/// <reference types="vitest/config" />
import fs from "node:fs";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Serve HTTPS in dev when a local cert exists — Plaid *production* OAuth (Chase, Capital One)
// rejects http:// redirect URIs, so the redirect target must be https://localhost:5173/.
// Generate the cert once:  mkcert -install && mkcert -cert-file certs/localhost.pem \
//   -key-file certs/localhost-key.pem localhost   (run from the frontend/ dir).
// Without the cert files this falls back to plain http, so sandbox dev still works.
const keyPath = "./certs/localhost-key.pem";
const certPath = "./certs/localhost.pem";
const https =
  fs.existsSync(keyPath) && fs.existsSync(certPath)
    ? { key: fs.readFileSync(keyPath), cert: fs.readFileSync(certPath) }
    : undefined;

// Dev server proxies API calls to the Spring Boot backend on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    https,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    css: false,
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
