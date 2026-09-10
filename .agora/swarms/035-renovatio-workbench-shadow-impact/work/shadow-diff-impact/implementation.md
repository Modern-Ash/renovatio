# Implementation

Implemented by PR #192:

- Backend DTO/service/controller for `GET /api/projects/{projectId}/workbench/shadow-impact`.
- Shadow diff report with planned, existing, added, removed, changed, and
  unresolved evidence entries.
- Source and artifact impact maps backed by source explorer, domain model, and
  architecture canvas state.
- Output-root-aware generated-target scan using `ProjectEntity.javaOutputPath`.
- Artifact trace links that remain empty/inferred when no domain evidence
  matches the artifact.
- Theia Shadow navigation area, export button, render states, and stale-response
  guard.
- Regression updates in `ArchitecturePreviewApiTest` and core-ui contract/build
  validation.

Merged commit: `ac4c8450fb6b724337c17b667fe216b85b71a7c5`.

