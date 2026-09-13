# Issue 205 Post-Refactor Slice

## Context

Issue #205 was written before the current COBOL reference pipeline existed. The implementation path should now follow the production pipeline:

1. COBOL parse
2. Semantic IR projection
3. Domain model projection
4. Architecture manifest
5. Java emission
6. Java build
7. Optional equivalence/golden comparison

The old `PopulateCobolProcessRecipe` remains part of the transpilation path, but it should not be the only ownership boundary for the epic. New COBOL semantics should be represented in the parser/IR/runtime/emitter path, with `ManualActionItem` used only for constructs that are deliberately not translated yet.

## First Viable Close

The first close for the epic is a real CardDemo batch program that runs through the production pipeline and produces buildable Java without unresolved generation markers.

Candidate selected: `CBACT02C`

Rationale:

- It is a real CardDemo batch program.
- The current CardDemo coverage report shows parse and emission success.
- The current CardDemo coverage report shows generated Java compilation success.
- It has no reported `TODO`, `Unhandled`, `COBOL not translated`, or `ManualActionItem` evidence.

## Acceptance Gate

For the first slice, the gate is:

- Parse succeeds.
- Semantic IR succeeds.
- Domain model succeeds.
- Architecture manifest succeeds.
- Emission succeeds.
- Generated Java compiles.
- No blocking semantic gaps are reported.
- Generated Java contains no unresolved markers:
  - `// TODO: Implement COBOL business logic`
  - `// Unhandled COBOL statement`
  - `// COBOL not translated:`

## Follow-Up Slices

After the first CardDemo batch gate is stable:

1. Add source-level golden/master fixture output for `CBACT02C`.
2. Move to `CBACT01C`, which currently does not compile in the CardDemo report.
3. Implement the missing COBOL subsets found by `CBACT01C` before expanding to CICS/DB2.
4. Treat CICS (#211) and DB2 (#212) as separate second-wave implementation tracks.
