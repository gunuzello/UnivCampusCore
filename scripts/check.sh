#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --wait
exists=$(docker compose exec -T postgres psql -U "${POSTGRES_USER:-ucc}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='ucc_test'")
if [ "$exists" != "1" ]; then docker compose exec -T postgres psql -U "${POSTGRES_USER:-ucc}" -d postgres -c 'CREATE DATABASE ucc_test'; fi
(cd backend && ./mvnw clean test)
(cd frontend && npm ci && npm run build)
