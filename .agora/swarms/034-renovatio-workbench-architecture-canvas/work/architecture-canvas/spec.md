# Theia 4 - Architecture Canvas and configurable profiles

## Outcome

The Renovatio Workbench SHALL expose a dedicated Architecture canvas where a
modernization engineer can choose, edit, validate, version, compare and restore
the target architecture profile before generation. The canvas SHALL support MVC,
Hexagonal, Clean, Layered and Transaction Script profiles, with Java/MVC defaults
for `model`, `service` and `controller` conventions.

This increment builds on the completed DomainModel editor from #179. Architecture
selection and naming SHALL consume project/profile/domain context but SHALL NOT
write, rewrite or re-project the persisted `DomainModel`.

## Profile model and persistence

An architecture profile SHALL contain target style, framework, persistence
strategy, module grouping, package roots, component suffixes, adapter settings,
class-name overrides and dependency rules. Saving a profile SHALL create an
immutable revision only when the canonical profile hash changes; stale writes
SHALL fail with a conflict instead of overwriting newer history.

Users SHALL be able to list profile versions, restore a historical version as a
new latest revision, compare two revisions, and see the reproducible canonical
hash that identifies the selected profile configuration.

## Canvas and validation

The Architecture canvas SHALL visualize the selected style as editable layers,
ports, adapters, controllers, services, models, repositories and/or script
nodes. Selecting a node SHALL expose labelled controls for package, suffix,
framework/persistence settings, class-name rules and dependency policy.

The canvas SHALL evaluate allowed and forbidden dependency rules live. Illegal
dependencies SHALL be visible before generation with source and target nodes,
severity and remediation text. Custom naming SHALL immediately recompute a shadow
preview of packages/classes and an artifact manifest without generating files.

## Workbench behavior

The UI SHALL reuse the existing workbench visual language with a dense,
industrial tool surface: architecture palette, central canvas, inspector, manifest
preview, validation strip and version/compare rail. It SHALL expose explicit
loading, ready, empty, permission-denied, conflict and error states.

Controls SHALL be keyboard operable and labelled; active selections SHALL use
`aria-current`/`aria-selected` where appropriate; save, conflict and validation
outcomes SHALL be announced through `aria-live`; visible focus SHALL be retained;
motion SHALL respect `prefers-reduced-motion`.

## Security and non-goals

Reads use the existing Workbench view authorization. Saves and restores require
the existing modify authorization, except for the already-configured local
development bypass. All operations are scoped to an existing project id.

This increment SHALL NOT trigger generation, modify source files, mutate the
DomainModel, accept LLM suggestions, deploy, or change unrelated dashboard flows.

## Verification

Verification SHALL cover architecture profile validation and hashing, MVC default
layout, custom naming manifest recomputation, illegal dependency diagnostics,
immutable version history, restore/compare behavior, API authorization/conflict
handling, frontend contracts, and a production workbench build.
