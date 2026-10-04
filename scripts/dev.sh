#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v python3 >/dev/null || { echo 'Python 3가 필요합니다.' >&2; exit 1; }
exec python3 scripts/dev.py
