# ADR-002: Python Provider Disposition

Date: 2026-09-11

## Status

Accepted

## Context

`renovatio-provider-python` exists as a small Python package with a COBOL
runtime/PIC mirror and focused pytest coverage. It does not implement the Java
`TargetEmitter` contract, is not part of the Maven reactor, does not emit target
artifacts, has no CLI/API/MCP/Workbench apply path, and has no generated-code
equivalence gate.

The root README and capability surface previously implied that Python was a
target alongside Java and Node. That was not truthful against the current
implementation.

## Decision

Keep `renovatio-provider-python` as an unsupported lab package for COBOL
runtime/PIC research. Do not integrate it as a supported or planned
COBOL-to-Python target in this cycle.

## Consequences

- Public capability metadata exposes `python.lab` with `unsupported` maturity on
  every surface.
- The root README describes Python as lab-only and states that it is not a
  `TargetEmitter`.
- Existing Python tests remain in CI to protect the preserved research code.
- Historical COBOL-to-Python docs remain available, but the current supported
  state is documented by the issue #234 reports.
- Future promotion to supported target status must implement canonical pipeline
  integration, `TargetEmitter` contract tests, packaging, fixtures,
  deterministic manifest and equivalence gates.
