# Evidence: Bootstrap Script

- **Type:** script
- **Result:** success
- **Date:** 2026-09-08
- **Command:** `./scripts/bootstrap.sh --skip-tests`
- **Environment:** Ubuntu 24.04, Java 21, Node 24.20.0, Python 3.14.4

## What it does

1. Verifies toolchain versions (Java 21, Node 24, Python 3.10+)
2. `./mvnw clean install` — builds all 21 Java modules
3. `cd renovatio-ui && npm ci && npm run build` — Vite SPA
4. `cd renovatio-workbench && npm ci && npm run build` — Theia workbench
5. `cd renovatio-provider-python && pip install -e ".[test]"` — Python provider
6. `cd specs/1-cobol-python-migration && pytest tests/` — migration spec tests

## Result

All components built successfully. Script is idempotent and fails on first error.
