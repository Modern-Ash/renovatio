#!/usr/bin/env bash
set -euo pipefail

# scripts/verify-epic-221-e2e.sh — End-to-end harness for Epic #221
# Usage:
#   ./scripts/verify-epic-221-e2e.sh [--quick] [--skip-bootstrap] [--skip-workbench-e2e]
#
# The full run verifies:
#   1. full repository bootstrap
#   2. COBOL -> Java reference path across provider, CLI, and API
#   3. 0.3.0-alpha release readiness metadata
#   4. Workbench build, smoke, and Playwright product e2e

RUN_BOOTSTRAP=true
RUN_WORKBENCH_E2E=true
QUICK=false

usage() {
  cat <<'USAGE'
scripts/verify-epic-221-e2e.sh — End-to-end harness for Epic #221
Usage:
  ./scripts/verify-epic-221-e2e.sh [--quick] [--skip-bootstrap] [--skip-workbench-e2e]

The full run verifies:
  1. full repository bootstrap
  2. COBOL -> Java reference path across provider, CLI, and API
  3. 0.3.0-alpha release readiness metadata
  4. Workbench build, smoke, and Playwright product e2e
USAGE
}

for arg in "$@"; do
  case "$arg" in
    --quick)
      QUICK=true
      RUN_BOOTSTRAP=false
      RUN_WORKBENCH_E2E=false
      ;;
    --skip-bootstrap)
      RUN_BOOTSTRAP=false
      ;;
    --skip-workbench-e2e)
      RUN_WORKBENCH_E2E=false
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n' "$arg" >&2
      exit 2
      ;;
  esac
done

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

log() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$1"; }
pass() { printf '\033[1;32m✓ %s\033[0m\n' "$1"; }
fail() { printf '\n\033[1;31mFAIL: %s\033[0m\n' "$1" >&2; exit 1; }

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 not found"
}

load_nvm_if_available() {
  if [[ -n "${NVM_DIR:-}" ]] || [[ -d "$HOME/.nvm" ]]; then
    export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
    # shellcheck source=/dev/null
    if [[ -s "$NVM_DIR/nvm.sh" ]]; then
      set +u
      source "$NVM_DIR/nvm.sh"
      set -u
    fi
  fi
}

log "Checking prerequisites"
require_command git
require_command java
require_command node
require_command npm
require_command python3
load_nvm_if_available
if command -v nvm >/dev/null 2>&1 && [[ -f renovatio-workbench/.nvmrc ]]; then
  REQUIRED_NODE="$(tr -d '[:space:]' < renovatio-workbench/.nvmrc)"
  set +u
  nvm use "$REQUIRED_NODE" >/dev/null || nvm install "$REQUIRED_NODE"
  set -u
fi
pass "toolchain commands available"

if [[ "$RUN_BOOTSTRAP" == true ]]; then
  log "Running full repository bootstrap"
  ./scripts/bootstrap.sh
  pass "bootstrap complete"
else
  log "Skipping full repository bootstrap"
  pass "bootstrap skipped"
fi

log "Running COBOL -> Java reference path across provider, CLI, and API"
if [[ "$QUICK" == true ]]; then
  ./mvnw -q \
    -pl renovatio-provider-cobol,renovatio-cli,renovatio-api -am \
    -Dtest=PipelineE2ETest,FixturesExistenceTest,RenovatioCliSmokeTest,ReferencePipelineApiTest \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Djacoco.skip=true \
    -Dexec.skip=true \
    test
else
  ./mvnw -q \
    -pl renovatio-provider-cobol,renovatio-cli,renovatio-api -am \
    -Dtest=PipelineE2ETest,PipelineValidationTest,SurfaceProofTest,FixturesExistenceTest,RenovatioCliSmokeTest,ReferencePipelineApiTest \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Djacoco.skip=true \
    -Dexec.skip=true \
    test
fi
pass "reference path verified"

log "Verifying 0.3.0-alpha release readiness"
./scripts/verify-alpha-release.sh
pass "release readiness verified"

if [[ "$RUN_WORKBENCH_E2E" == true ]]; then
  log "Running Workbench build, smoke, and product e2e"
  (
    cd renovatio-workbench
    npm ci
    npm run build
    npm test
    npm run smoke
    npm run e2e:install
    npm run e2e
  )
  pass "workbench e2e verified"
else
  log "Skipping Workbench product e2e"
  pass "workbench e2e skipped"
fi

log "Epic #221 verification complete"
pass "Epic #221 e2e harness passed"
