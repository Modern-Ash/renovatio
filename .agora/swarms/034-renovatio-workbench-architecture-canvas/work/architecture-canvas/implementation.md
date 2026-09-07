# Implementation summary

Issue #180 adds a governed Architecture Canvas for the Theia 4 workbench.

## Backend

- Added architecture style support for `LAYERED_MVC`, `LAYERED`, and `CLEAN` in the profile enum and JSON schema.
- Added a layered architecture transformer profile that emits model, service, controller, repository/adapter, and dependency relations without generating source artifacts.
- Added versioned architecture profile persistence through `project_architecture_profile_versions`.
- Added architecture canvas DTOs, repository, service, schema initialization, controller endpoints, and scoped error handling.
- Architecture profile saves are revision-checked and update only profile state; domain model versions remain untouched.
- Saved profiles include canonical hashes, compare support, restore-as-new-revision support, dependency diagnostics, and manifest preview.

## Workbench UI

- Replaced the read-only architecture summary with a configurable canvas for style, module grouping, naming, packages, classes, and dependency rules.
- Added live canvas layers, manifest preview, dependency diagnostics, history restore, and profile compare affordances.
- Added states for saving, revision conflicts, permission denied, refresh, and dirty draft tracking.

## Tests

- Added service-level coverage for immutability, MVC defaults, custom naming/shadow manifest, dependency diagnostics, versioning, compare, restore, and stale-write rejection.
- Updated architecture transformer/API preview expectations for active layered profiles.
- Extended workbench contract tests for endpoints, save/compare/restore flows, supported styles, UI states, and dependency-rule affordances.
