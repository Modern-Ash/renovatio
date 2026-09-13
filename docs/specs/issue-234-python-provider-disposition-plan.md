# Issue #234 Implementation Plan

## Decision Path

Choose the unsupported lab option because the current package has useful PIC
runtime mirror work but lacks every production emitter integration requirement.

## Implementation Steps

1. Update `SurfaceCapabilityRegistry` from `python.target` planned generation to
   `python.lab` unsupported research.
2. Update API, CLI and application capability tests to assert the unsupported
   capability state.
3. Update the root README so architecture and capabilities do not advertise a
   Python emitter.
4. Add a package README that states current lab scope and unsupported
   boundaries.
5. Record ADR, capability assessment, disposition report and test report.
6. Keep Python CI jobs intact for preserved lab behavior.

## Verification

- Run focused Java capability tests.
- Run Python lab pytest suite.
- Run `git diff --check`.
