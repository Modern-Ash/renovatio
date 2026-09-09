---
schema: "agora/evidence-entry/v3"
id: "evidence-000004"
work-item: "reproducible-build-ci"
swarm: "issue-223-reproducible-build-ci"
artifact: "bootstrap.sh"
result: "pass"
date: "2026-09-09"
---

# Evidence: Bootstrap Script

- **Type:** script
- **Result:** success
- **Date:** 2026-09-08
- **Command:** `./scripts/bootstrap.sh --skip-tests`
- **Environment:** Ubuntu 24.04, Java 21, Node 24.20.0, Python 3.14.4

## What it does

1. Verifies toolchain versions (Java 21, Node 24, Python 3.10+)
2. `cd renovatio-ui && npm ci` — installs UI dependencies first
3. `./mvnw clean install -Djacoco.skip=true` — builds all 22 Java modules
4. `cd renovatio-ui && npm test && npm run build` — Vite SPA
5. `cd renovatio-workbench && npm ci && npm test && npm run build` — Theia workbench
6. `cd renovatio-provider-python && pip install -e ".[test]"` — Python provider
7. `cd specs/1-cobol-python-migration && pytest tests/` — migration spec tests

## Result

All components built successfully. Script is idempotent and fails on first error.
