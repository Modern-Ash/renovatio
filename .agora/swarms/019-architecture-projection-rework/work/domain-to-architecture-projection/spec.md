# DomainModel to ArchitectureModel specification

## Outcome

Introduce an explicit architecture projection over the confirmed `DomainModel`. Architecture is a deterministic view of business structure; it must not re-parse COBOL or let an LLM emit source code.

## Projection rules

- Transaction Script maps each use case to a minimal application service and preserves existing program-oriented layout.
- Layered MVC is active only when controller/service/repository layout and tests exist; otherwise it remains unavailable in profile/UI.
- Hexagonal maps use cases to application services, domain nodes to core components, and repository/external-system boundaries to ports/adapters. Unsupported or unproven boundaries produce per-program fallbacks and diagnostics.
- Every component and relation retains DomainModel provenance and stable IDs.

## Compatibility

The projection adapts to the existing `ArchitectureResult`/`TargetModel` contracts where possible and keeps the current Semantic IR path unchanged for projects without a DomainModel.

## Acceptance evidence

Contract tests, per-style layout tests, fallback tests, deterministic hash tests, emitter compatibility tests, Maven regression, and diff hygiene.
