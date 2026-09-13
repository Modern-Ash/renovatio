# Issue #234 Spec: Python Provider Disposition

## Objective

Resolve the current state of `renovatio-provider-python` without presenting it
as a supported COBOL-to-Python target until it satisfies the canonical target
contract.

## Scope

- Audit current Python package code, tests, dependencies and CI treatment.
- Compare the package against the Java `TargetEmitter` contract and canonical
  application pipeline requirements.
- Record an ADR choosing integration, unsupported lab, or retirement.
- Update public capability metadata and documentation to match the chosen state.
- Keep CI aligned with the chosen state.

## Out Of Scope

- Implementing a Python `TargetEmitter`.
- Expanding COBOL-to-Python generation.
- Removing historical documents that may still be useful as research material.

## Acceptance Criteria

- `assessment`: capability inventory documents real Python package behavior and
  gaps against `TargetEmitter`.
- `decision`: ADR records the chosen unsupported lab disposition with cost,
  benefit and consequences.
- `truthfulness`: README and capability schema no longer present Python as a
  supported or planned target.
- `integration-option`: explicitly not selected; promotion requirements are
  documented.
- `archive-option`: history is preserved and misleading entrypoints are removed
  from supported surfaces.
- `ci-scope`: CI continues running Python lab tests and security audit without
  treating the package as an emitter gate.
