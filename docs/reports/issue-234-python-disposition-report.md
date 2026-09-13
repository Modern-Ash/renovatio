# Issue #234 Python Disposition Report

Date: 2026-09-11

## Selected Disposition

`renovatio-provider-python` remains in the repository as an unsupported lab
package for COBOL runtime/PIC research.

## Changes Applied

- Root README now states Python is lab-only and not a `TargetEmitter`.
- Capability registry now exposes `python.lab` as `unsupported` on API, CLI, MCP
  and Workbench surfaces.
- Capability tests now verify the unsupported state.
- Python package metadata no longer claims COBOL-to-Python generation.
- `renovatio-provider-python/README.md` documents current scope and promotion
  requirements.
- ADR-002 records the decision and consequences.

## Future Promotion Requirements

Python can become a supported target only after implementing:

- Java `TargetEmitter` contract for `MigrationProfile.Language.PYTHON`.
- Canonical pipeline routing through application services.
- Deterministic emitted artifact manifest.
- Packaging/build integration.
- Contract tests, fixtures and equivalence gates.
- CLI/API/MCP/Workbench capability behavior consistent with other targets.

## CI Scope

The Python lab package remains tested by the existing Python CI job and
`pip-audit` workflow step. This is intentionally a lab-health gate, not a target
emitter equivalence gate.
