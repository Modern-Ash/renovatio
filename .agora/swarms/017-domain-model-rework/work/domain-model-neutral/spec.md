# DomainModel neutral specification

## Outcome

Define a versioned, target-neutral business model between `SemanticProgram` and architecture projection. The model is an auditable representation of business meaning, not a UML file and not generated source code.

## Contract

`DomainModel v1` contains deterministically ordered entities, value objects, aggregates, use cases, domain services, repositories, external systems, events, and business invariants. Every node carries a stable id, display name, semantic kind, source references, provenance, confidence, and origin (`DETERMINISTIC`, `LLM`, or `HUMAN`).

## Rules

- A node without inspectable COBOL/Semantic IR evidence or an explicit human decision is not accepted.
- References and relationships are validated for existence, uniqueness, and compatible kinds.
- Canonical serialization and SHA-256 identity make identical input reproducible.
- Multiple programs and shared copybooks are supported without cross-project leakage.
- Existing projects without a DomainModel retain the current Semantic IR → Architecture → TargetModel path.

## Out of scope

COBOL-to-domain inference, LLM prompts, UML rendering, architecture projections, and emitter changes are separate work items (#170 onward).

## Acceptance evidence

Schema/contract tests, canonical round-trip tests, multi-program identity tests, validation failure tests, and diff hygiene.
