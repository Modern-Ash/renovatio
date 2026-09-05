#!/usr/bin/env bash
set -euo pipefail
: "${RENOVATIO_CANDIDATE_BIN:?Set RENOVATIO_CANDIDATE_BIN to the generated executable}"
exec "$RENOVATIO_CANDIDATE_BIN" "$@"
