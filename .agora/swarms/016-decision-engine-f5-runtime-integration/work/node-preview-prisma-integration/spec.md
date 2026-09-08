# F5 runtime integration specification

## Outcome

Node preview and emission must operate on the selected project's real COBOL workspace and effective migration profile. Prisma persistence output and Node idiom handling must be part of the production pipeline, while existing deterministic multi-program behavior remains stable.

## Boundaries

- Preview is read-only: it resolves `ProjectEntity`, builds `Workspace`, parses the workspace through the existing COBOL generation service, and emits Node artifacts without writing generated files.
- Persistence integration is limited to strategies and artifacts already represented by the profile and registry; no new remote services or marketplace behavior.
- Unsupported COBOL constructs remain explicit manual action items; they are never silently discarded.

## Acceptance mapping

1. Project-backed preview: project lookup, effective profile, real semantic programs, and per-program output.
2. Prisma artifacts: deterministic schema/repository/config/seed paths when Prisma is selected, with safe shared-artifact deduplication.
3. Idiom integration: production renderer consults the catalog and emits manual-action metadata for unsupported constructs.
4. Multi-program compatibility: stable ordering, collision detection, and shared files remain deterministic.
5. Regression quality: focused tests, Maven compile/test, and `git diff --check` evidence.

## Risks and safeguards

Missing or inaccessible workspaces fail with the existing preview error contract. Strategy mismatches fail closed with a visible action item or explicit error rather than falling back silently. Shared artifacts are inserted once and conflicting content is rejected.
