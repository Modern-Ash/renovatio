# Issue 230 Surface Capabilities Contract

Issue #230 unifies API, CLI, MCP and Workbench-facing capability discovery under an application-owned contract.

## Contract

- Contract id: `renovatio.surface-capabilities`
- Version: `2026-09-11.ac13`
- Owner: `renovatio-application`
- Surfaces: `api`, `cli`, `mcp`, `workbench`

The contract is implemented by `SurfaceCapabilityRegistry` in `renovatio-application`. API, CLI and MCP consume that registry directly rather than maintaining independent capability lists.

## Public Surfaces

- API: `GET /api/v1/capabilities`
- CLI: `renovatio capabilities [--json]`
- MCP: `renovatio_capabilities` (`renovatio.capabilities` remains accepted as an execution alias)

## Maturity Semantics

- `stable`: implemented and supported on the listed surface.
- `experimental`: available for preview usage, not guaranteed as a stable workflow.
- `planned`: intentionally not advertised as runnable.
- `unsupported`: preserved or documented for compatibility/research only, not a runnable product capability.

The registry currently marks the COBOL-to-Java path and reference pipeline as stable, Node/JCL work as experimental or planned, and the Python lab package as unsupported research rather than target generation.

Each capability also declares authorization, API job lifecycle states (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`), error codes, manifest hash fields, and runtime support for LLM, persistence and equivalence. Surface support is declared per capability and adapter; stable capabilities may still be `planned` on surfaces that do not expose a runnable command or endpoint yet.
