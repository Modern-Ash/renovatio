# Theia 9 · Hardening, distribución y adopción

## Objective

Prepare Renovatio Workbench for continuous web use while preserving the existing dashboard and governed backend contracts.

## Scope

- Reproducible web distribution through pinned Node/npm/Theia versions and Docker build validation.
- CI gates for extension/UI contracts, API workbench regressions, E2E HTTP smoke, hardening audit, performance budgets, demo pilot and production dependency audit.
- Security posture for workspace isolation, CSP, telemetry opt-in, Renovatio command allowlist and MCP allowlist.
- Versioned compatibility matrix for Theia, Node, npm, Open VSX and optional desktop packaging.
- User migration and operating docs, including an independent LLM continuation guide.
- Hash-validated pilot with demo COBOL programs and IR fixtures.

## Non-goals

- Shipping a desktop/Electron distribution.
- Enabling Open VSX/plugin runtime dependencies.
- Enabling telemetry by default.
