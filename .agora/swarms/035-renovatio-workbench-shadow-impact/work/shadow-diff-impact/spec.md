# Theia 5 — Shadow, diff e impact analysis

## Scope

GitHub #181 delivers a read-only Shadow workbench area before generation. The
area compares the architecture manifest against generated target files, reports
added/removed/changed artifacts, links source/domain/artifact impact evidence,
and exports a canonical JSON report without mutating project sources or backend
generation flows.

## Acceptance criteria

- Existing generated targets are scanned relative to the configured Java output
  root, so manifest paths and workspace paths are compared in the same namespace.
- Artifact provenance is truthful: unmatched artifacts do not inherit unrelated
  domain evidence, and deterministic links require matched domain evidence.
- Shadow responses are guarded against stale project switches in the Theia UI.
- UI states cover loading, empty, permission-denied, error, refresh, and JSON
  export.
- Backend, UI, smoke, characterization, and review-thread checks pass before
  closing GitHub #181.

