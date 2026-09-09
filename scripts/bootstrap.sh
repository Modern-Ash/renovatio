#!/usr/bin/env bash
set -euo pipefail

# scripts/bootstrap.sh — Reproducible build from clean clone
# Usage: ./scripts/bootstrap.sh [--skip-tests]
#
# Prerequisites:
#   - Java 21+ (JAVA_HOME or PATH)
#   - Node.js 24.20.0 (nvm recommended, see renovatio-workbench/.nvmrc)
#   - Python 3.10+ with venv support
#
# What it does:
#   1. Verifies toolchain versions
#   2. Builds Java reactor via Maven Wrapper
#   3. Builds renovatio-ui (Vite SPA)
#   4. Builds renovatio-workbench (Theia)
#   5. Builds renovatio-provider-python
#   6. Runs tests for each component

SKIP_TESTS=false
if [[ "${1:-}" == "--skip-tests" ]]; then
  SKIP_TESTS=true
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

log() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$1"; }
fail() { printf '\n\033[1;31mFAIL: %s\033[0m\n' "$1" >&2; exit 1; }

# ─── 1. Verify toolchains ───────────────────────────────────────────

log "Verifying toolchains"

# Java
if ! command -v java &>/dev/null; then
  fail "java not found. Install JDK 21+."
fi
JAVA_VERSION=$(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')
if [[ "$JAVA_VERSION" -lt 21 ]]; then
  fail "Java $JAVA_VERSION found, need 21+."
fi
echo "  Java: $JAVA_VERSION ✓"

# Node — try nvm first, then system
if [[ -n "${NVM_DIR:-}" ]] || [[ -d "$HOME/.nvm" ]]; then
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  # shellcheck source=/dev/null
  [[ -s "$NVM_DIR/nvm.sh" ]] && source "$NVM_DIR/nvm.sh"
fi

REQUIRED_NODE="24.20.0"
if command -v nvm &>/dev/null; then
  nvm use "$REQUIRED_NODE" 2>/dev/null || nvm install "$REQUIRED_NODE"
elif [[ -f "$REPO_ROOT/renovatio-workbench/.nvmrc" ]]; then
  CURRENT_NODE=$(node --version 2>/dev/null | sed 's/v//')
  if [[ "$CURRENT_NODE" != "$REQUIRED_NODE" ]]; then
    echo "  WARNING: Node $CURRENT_NODE found, recommend $REQUIRED_NODE"
    echo "  Install nvm and run: nvm install $REQUIRED_NODE"
  fi
fi

if ! command -v node &>/dev/null; then
  fail "node not found. Install Node.js $REQUIRED_NODE."
fi
NODE_VERSION=$(node --version | sed 's/v//')
echo "  Node: $NODE_VERSION ✓"

if ! command -v npm &>/dev/null; then
  fail "npm not found."
fi
NPM_VERSION=$(npm --version)
echo "  npm: $NPM_VERSION ✓"

# Python
if ! command -v python3 &>/dev/null; then
  fail "python3 not found. Install Python 3.10+."
fi
PYTHON_VERSION=$(python3 --version | sed 's/Python //')
PYTHON_MAJOR=$(echo "$PYTHON_VERSION" | cut -d. -f1)
PYTHON_MINOR=$(echo "$PYTHON_VERSION" | cut -d. -f2)
if [[ "$PYTHON_MAJOR" -lt 3 ]] || { [[ "$PYTHON_MAJOR" -eq 3 ]] && [[ "$PYTHON_MINOR" -lt 10 ]]; }; then
  fail "Python $PYTHON_VERSION found, need 3.10+."
fi
echo "  Python: $PYTHON_VERSION ✓"

# ─── 2. Install UI dependencies (needed by renovatio-api Maven exec) ─

log "Installing renovatio-ui dependencies"
(
  cd renovatio-ui
  npm ci
)

# ─── 3. Maven build ─────────────────────────────────────────────────

log "Building Java reactor"
if [[ "$SKIP_TESTS" == true ]]; then
  ./mvnw clean install -DskipTests -Djacoco.skip=true
else
  ./mvnw clean install -Djacoco.skip=true
fi

# ─── 4. Build renovatio-ui ──────────────────────────────────────────

log "Building renovatio-ui"
(
  cd renovatio-ui
  if [[ "$SKIP_TESTS" == true ]]; then
    npm run build
  else
    npm test
    npm run build
  fi
)

# ─── 5. Build renovatio-workbench ───────────────────────────────────

log "Building renovatio-workbench"
(
  cd renovatio-workbench
  npm ci
  if [[ "$SKIP_TESTS" == true ]]; then
    npm run build
  else
    npm test
    npm run build
  fi
)

# ─── 6. Build Python ────────────────────────────────────────────────

log "Building renovatio-provider-python"
(
  cd renovatio-provider-python
  python3 -m venv .venv
  # shellcheck source=/dev/null
  source .venv/bin/activate
  pip install -e ".[test]"
  if [[ "$SKIP_TESTS" == true ]]; then
    echo "  Skipping Python tests (--skip-tests)"
  else
    pytest
  fi
)

# ─── 7. Build migration spec Python ────────────────────────────────

log "Building migration spec Python"
(
  cd specs/1-cobol-python-migration
  bash scripts/setup_env.sh
  # shellcheck source=/dev/null
  source .venv/bin/activate
  if [[ "$SKIP_TESTS" == true ]]; then
    echo "  Skipping migration spec tests (--skip-tests)"
  else
    pytest tests/
  fi
)

# ─── Done ────────────────────────────────────────────────────────────

log "Bootstrap complete"
echo "  Java reactor:   ✓"
echo "  renovatio-ui:   ✓"
echo "  workbench:      ✓"
echo "  Python:         ✓"
echo "  Migration spec: ✓"
