#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --wait
if [ ! -d frontend/node_modules ]; then npm --prefix frontend ci; fi
(cd backend && ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=local) &
backend_pid=$!
cleanup() { kill "$backend_pid" "${frontend_pid:-}" 2>/dev/null || true; }
trap cleanup EXIT INT TERM
(cd frontend && npm run dev -- --port 5173 --strictPort) &
frontend_pid=$!
wait
