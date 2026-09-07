# Review report · Source Explorer COBOL/JCL/Copybooks (issue #178)

## Scope and boundary

The Source Explorer adds one read-only endpoint
(`GET /api/projects/{id}/workbench/source-explorer`) and a Project-area panel in
the existing shell widget. `WorkbenchSourceExplorerService` walks only the
project's normalized workspace root, performs deterministic regex line-scanning,
and returns structure, symbols, per-file `hash`/`encoding`/`analysisStatus`,
parse diagnostics and JCL dataset aggregation. It has no write path, triggers no
analysis, and does not touch the semantic pipeline, the React dashboard or the
wizard. The frontend never issues a mutating request to the endpoint
(asserted in `contract.test.mjs`).

## Acceptance criteria

All eight criteria reached `verified` with inspectable support in
`verification-report.md`: navigable-tree, symbol-outline, search,
file-metadata, ir-diagnostic-linkage, resilient-unsupported, review-boundary,
verification-evidence.

## Findings

- **Info — deterministic `irCoordinate` is a reference, not a live IR fetch.**
  The contract documents this explicitly; live IR document rendering is a
  named follow-up.
- **Info — interactive/macOS capture deferred.** Same Node 24 / platform
  constraint recorded for #176 and #177. Backend tests, contract tests and a
  0-error production Theia bundle stand as the automated evidence.

## Gate decision

Acceptable for completion as a governed read-only increment. The two info
findings are release-level follow-ups, not blockers, and match the boundary
already accepted for the preceding Workbench area swarms.
