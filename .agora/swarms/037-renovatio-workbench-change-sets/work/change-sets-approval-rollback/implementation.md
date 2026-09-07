# Implementation

Implemented in branch `agora/theia-change-sets`.

Backend:

- Added `WorkbenchChangeSetDto`.
- Added `WorkbenchChangeSetService` with in-process project-scoped change set storage, manifest hashing, diff view tracking, state enforcement, approval, apply, reject, rollback, and audit events.
- Added `/api/projects/{projectId}/workbench/change-sets` endpoints for list, create, read, diff, submit-review, approve, reject, apply, and rollback.
- Apply and rollback use the existing `WorkbenchProjectAdapterService` generated-target write boundary.

Frontend:

- Added the `changes` workbench activity area.
- Added command/keybinding/menu entry for Change Sets.
- Added a renderer showing manifest hashes, mandatory diffs, approval requirements, exact confirmation phrases, state-specific actions, and audit history.
