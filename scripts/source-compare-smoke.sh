#!/usr/bin/env bash
set -euo pipefail
: "${RENOVATIO_API_URL:=http://localhost:8080}"
: "${RENOVATIO_PROJECT_ID:?Set RENOVATIO_PROJECT_ID}"
curl -fsS -X POST "${RENOVATIO_API_URL}/api/projects/${RENOVATIO_PROJECT_ID}/equivalence/source-compare" \
  -H 'Content-Type: application/json' \
  --data-binary @docs/equivalence-fixtures/source-compare-smoke.json
