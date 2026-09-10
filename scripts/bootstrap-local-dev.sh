#!/usr/bin/env bash
set -euo pipefail

REQUIRED_CMDS=(java mvn docker git)

for cmd in "${REQUIRED_CMDS[@]}"; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
done

echo "== Tool versions =="
java -version 2>&1 | sed -n '1,2p'
mvn -version | sed -n '1,4p'
docker --version
git --version

echo "== Running full verification build =="
mvn -B -ntp clean verify

echo "Bootstrap complete. Local environment is ready."
