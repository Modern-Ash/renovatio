# Evidence: Node Build

- **Type:** build
- **Result:** success
- **Date:** 2026-09-08
- **Environment:** Ubuntu 24.04, Node 24.20.0, npm 11.19.0

## renovatio-ui

- **Command:** `cd renovatio-ui && npm ci && npm test && npm run build`
- **Tests:** 28 passed, 0 failed
- **Build:** Vite production build → renovatio-api/src/main/resources/static/

## renovatio-workbench

- **Command:** `cd renovatio-workbench && npm ci && npm test && npm run build`
- **Tests:** 21 passed, 0 failed
- **Build:** Theia production build

## Changes made

- Added .nvmrc to renovatio-ui (Node 24.20.0)
- Fixed node-idioms.md collision: removed from DefaultNodeRenderer (no longer generates shared artifact per-program)
- Updated NodeEmitterTest to verify package.json is shared and node-idioms.md is not generated
