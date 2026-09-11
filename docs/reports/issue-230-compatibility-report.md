# Issue 230 Compatibility Report

The implementation is additive.

- Existing API routes are unchanged.
- Existing CLI commands are unchanged; `capabilities` is a new subcommand.
- Existing MCP tools remain provider-derived; `renovatio_capabilities` is appended as an additional discovery tool and is preserved under language filtering.
- Unsupported or incomplete targets are represented as `planned`, avoiding false runnable claims.

Known compatibility note: API module test/build execution runs the UI `npm run build` during `generate-resources`. In the current environment `vite` is unavailable because UI dependencies are not installed, so endpoint verification used `-Dexec.skip=true`.

## Workbench And UI Decision

`renovatio-workbench` is the converged operator experience for project-level modernization work. It remains the primary Workbench surface and consumes public Spring Boot endpoints only, including `/api/v1/capabilities`.

`renovatio-ui` is retained temporarily through 2026-12-31 for the administrative dashboard and legacy 8-step wizard. Its critical route coverage remains under `renovatio-ui/src/api/__tests__/client.test.js`; the client now exposes `getCapabilities()` against `/api/v1/capabilities` so routing can be gated by the same public contract. No new direct wizard internals are added.

## Boundary Search

Observed adapter paths:

- API controllers remain request/response adapters over services or the shared capability registry.
- CLI commands route through `ApplicationCommandBus` or read the shared capability registry.
- MCP execution routes through `ApplicationCommandBus`; `renovatio_capabilities` returns the shared registry without provider-side projection.
- Workbench code uses `fetch` against `/api/...` endpoints and does not import `renovatio-ui/src`, wizard classes, or Spring services.
