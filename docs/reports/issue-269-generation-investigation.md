# Issue 269 generation path investigation

Date: 2026-09-12

## Findings

- `WorkbenchChangeSetService` already owns the Workbench review flow: draft creation, diff viewing, submit-review, approve, apply, reject and rollback.
- `JavaGenerationService` exists in `renovatio-provider-cobol` and is already used by `ArchitecturePreviewService` and node preview paths, but there was no Workbench endpoint that turned a saved Architecture profile into a `WorkbenchChangeSet`.
- The Architecture canvas service already has the governed manifest (`WorkbenchArchitectureCanvasDto.manifest`) and dependency diagnostics. Issue 268 adds `excludedNodeIds` to the saved architecture profile, so the manifest can be filtered before creating a change set.

## Implemented path

- Added `POST /api/projects/{projectId}/workbench/architecture/canvas:generate`.
- The endpoint requires a saved architecture profile revision, rejects blocking dependency diagnostics, filters manifest entries whose `componentId` is in `excludedNodeIds`, and creates a draft `WorkbenchChangeSet` through the existing `WorkbenchChangeSetService`.
- Generated file contents are manifest-driven Java stubs that preserve package, class, layer, role, component id, architecture revision and architecture hash. Full semantic Java generation can replace the stub body later without changing the Workbench review/apply flow.

## Follow-up

- Wire the manifest entries to the consolidated Epic #205 code-generation output when that service exposes a Workbench-compatible artifact payload.
