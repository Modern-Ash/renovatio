# Issue 230 Surface Capabilities Contract

Issue #230 unifies API, CLI, MCP and Workbench-facing capability discovery under an application-owned contract.

## Contract

- Contract id: `renovatio.surface-capabilities`
- Version: `2026-09-11.ac09`
- Owner: `renovatio-application`
- Surfaces: `api`, `cli`, `mcp`, `workbench`

The contract is implemented by `SurfaceCapabilityRegistry` in `renovatio-application`. API, CLI and MCP consume that registry directly rather than maintaining independent capability lists.

## Public Surfaces

- API: `GET /api/v1/capabilities`
- CLI: `renovatio capabilities [--json]`
- MCP: `renovatio.capabilities`

## Maturity Semantics

- `stable`: implemented and supported on the listed surface.
- `experimental`: available for preview usage, not guaranteed as a stable workflow.
- `planned`: intentionally not advertised as runnable.

The registry currently marks the COBOL-to-Java path and reference pipeline as stable, Node/JCL work as experimental or planned, and Python target generation as planned.

Each capability also declares authorization, normalized lifecycle states, error codes, manifest hash fields, and runtime support for LLM, persistence and equivalence.
