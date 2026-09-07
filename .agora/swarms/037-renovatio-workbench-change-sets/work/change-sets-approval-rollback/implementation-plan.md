# Implementation plan

1. Add a Workbench change set DTO representing manifests, diffs, file hashes, state, and audit history.
2. Add a service that enforces legal transitions, mandatory diff review, manifest match, role-mediated actions, and rollback.
3. Expose project-scoped endpoints under `/workbench/change-sets`.
4. Route writes through `WorkbenchProjectAdapterService` so only generated targets in development write mode can mutate.
5. Add backend tests for state machine, permissions, rejected no-op, apply, rollback, and history.
6. Add Theia UI contract for the new Change Sets area.
