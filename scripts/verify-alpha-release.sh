#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-0.3.0-alpha.1}"

fail() {
  printf 'release verification failed: %s\n' "$1" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "missing required file: $1"
}

require_absent_tracked() {
  local path="$1"
  if git ls-files --error-unmatch "$path" >/dev/null 2>&1; then
    fail "tracked forbidden artifact: $path"
  fi
}

require_rg_absent() {
  local pattern="$1"
  shift
  if rg -n --hidden --glob '!.git/**' --glob '!renovatio-ui/package-lock.json' --glob '!renovatio-workbench/package-lock.json' "$pattern" "$@" >/tmp/renovatio-alpha-rg.txt; then
    cat /tmp/renovatio-alpha-rg.txt >&2
    fail "unexpected pattern '$pattern'"
  fi
}

require_file LICENSE
require_file README.md
require_file ARCHITECTURE.md
require_file CONTRIBUTING.md
require_file SECURITY.md
require_file CHANGELOG.md
require_file "docs/release/${VERSION}-capability-matrix.md"
require_file "docs/release/${VERSION}-release-notes.md"
require_file "docs/release/${VERSION}-sbom.md"
require_file "docs/release/${VERSION}-provenance.md"
require_file "docs/release/${VERSION}-troubleshooting.md"
require_file "docs/release/${VERSION}-checksums.txt"
require_file "docs/reports/issue-235-release-readiness-report.md"

[[ "$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)" == "$VERSION" ]] \
  || fail "root Maven version is not $VERSION"

node -e "const fs=require('fs'); for (const f of ['renovatio-ui/package.json','renovatio-workbench/package.json','renovatio-workbench/applications/browser/package.json','renovatio-workbench/extensions/renovatio-core-ui/package.json']) { const p=JSON.parse(fs.readFileSync(f)); if (p.version !== '$VERSION') throw new Error(f+' version '+p.version); }"
node -e "const fs=require('fs'); const p=JSON.parse(fs.readFileSync('renovatio-workbench/applications/browser/package.json')); if (p.dependencies['@renovatio/core-ui'] !== '$VERSION') throw new Error('@renovatio/core-ui dependency '+p.dependencies['@renovatio/core-ui']);"

require_absent_tracked data/renovatio-db.mv.db
require_absent_tracked renovatio-provider-cobol/target_bad/classes/app-input.json
require_absent_tracked renovatio-provider-cobol/target_bad/test-classes/app-input.json

require_rg_absent '0\.0\.1-SNAPSHOT' \
  pom.xml \
  */pom.xml \
  renovatio-ui/package.json \
  renovatio-workbench/package.json \
  renovatio-workbench/applications/browser/package.json \
  renovatio-workbench/extensions/renovatio-core-ui/package.json \
  docs/release \
  docs/reports/issue-235-release-readiness-report.md
require_rg_absent 'draft-blocked|blocked-by-dependencies|pending-generation|AC-03 / issue #224 is still blocked' \
  docs/release \
  docs/reports/issue-235-release-readiness-report.md \
  docs/specs/issue-235-community-alpha-release.md \
  docs/specs/issue-235-community-alpha-release-plan.md

sha256sum \
  "docs/release/${VERSION}-capability-matrix.md" \
  "docs/release/${VERSION}-community-docs.md" \
  "docs/release/${VERSION}-provenance.md" \
  "docs/release/${VERSION}-release-notes.md" \
  "docs/release/${VERSION}-release-plan.md" \
  "docs/release/${VERSION}-sbom.md" \
  "docs/release/${VERSION}-troubleshooting.md" \
  "docs/reports/issue-235-release-readiness-report.md" \
  | diff -u - "docs/release/${VERSION}-checksums.txt"

printf 'release verification passed for %s\n' "$VERSION"
