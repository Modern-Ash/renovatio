#!/usr/bin/env bash
set -euo pipefail
: "${RENOVATIO_BASELINE_BIN:?Set RENOVATIO_BASELINE_BIN to the COBOL executable}"
exec "$RENOVATIO_BASELINE_BIN" "$@"
