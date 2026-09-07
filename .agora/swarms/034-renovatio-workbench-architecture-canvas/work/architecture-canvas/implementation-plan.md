# Implementation plan - Architecture Canvas (#180)

## Backend and contracts

1. Extend the architecture/profile domain with a configurable architecture profile
   contract for the five styles, Java framework/persistence settings, package
   roots, suffixes, adapters, class-name overrides and dependency rules.
2. Add deterministic canonicalization and hashing for saved profiles and
   generated preview payloads. Preserve sparse profile defaults and never route
   profile edits through DomainModel persistence.
3. Add an architecture workbench service that returns the current profile,
   computes the shadow package/class manifest, validates dependencies, saves
   immutable revisions with optimistic revision checks, restores historical
   revisions and compares versions.
4. Expose project-scoped Architecture Canvas API routes from the workbench layer,
   reusing `canView` for reads and `canModify` for mutations.
5. Add focused Java tests for MVC defaults, custom naming recalculation,
   dependency diagnostics, immutable history, conflict handling and DomainModel
   immutability.

## Frontend

6. Add/extend the Architecture activity area in the Theia workbench shell so a
   selected project opens a dedicated canvas instead of a static preview.
7. Build a dense industrial interface with style selector, editable canvas nodes,
   inspector controls, dependency validation strip, manifest/package preview, and
   history compare/restore rail.
8. Keep all controls accessible: labelled inputs, keyboard-sized buttons, stable
   focus, reduced-motion support and `aria-live` announcements for save,
   conflicts and validation changes.
9. Extend frontend API adapters and tests for the new routes, explicit UI states,
   profile changes, preview recomputation, validation diagnostics and version
   operations.

## Verification and governance

10. Run focused backend tests, frontend contract/unit tests, workbench production
    build and HTTP smoke checks where the environment permits.
11. Register implementation and verification artifacts/evidence, advance criteria
    through implemented and verified, and leave final acceptance/issue closure for
    the Spec Owner after review.
