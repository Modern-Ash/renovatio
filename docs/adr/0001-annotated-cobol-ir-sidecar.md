# ADR 0001: Versioned annotated COBOL IR sidecar

- Status: Accepted
- Date: 2026-08-30
- Issue: [#124](https://github.com/Modern-Ash/renovatio/issues/124)
- Agora work: `ai-modernization/annotated-ir-contract`

## Context

Residual COBOL modernization needs reviewable LLM suggestions without making an LLM the source of
core semantics or introducing provider calls into OpenRewrite recipes. The result must remain
auditable, reproducible offline, and safely associated with the exact deterministic IR it enriches.

## Decision

Use an additive `cobol-annotated-ir.v1` sidecar. It references, but never embeds or mutates, the
`cobol-ir.v1` document. The in-memory `AnnotatedCobolContext` pairs a validated sidecar with the exact
base-model instance only at the orchestration boundary.

All content identities use lowercase SHA-256 over RFC 8785 canonical JSON projections. Annotation
and cache identities are domain-separated with `identityType` values `annotation` and `cache`.
Review metadata, provider envelopes, timestamps, and cache disposition do not change proposal
identity. Validated payload and confidence form a separate output hash so conflicting outputs for a
single proposal identity fail closed.

The persisted JSON Schema is strict JSON Schema 2020-12: every owned object is closed, annotation
families have discriminated typed payloads, and review snapshots have explicit state-dependent
fields. Cross-document properties such as base hashes, node resolution, kind agreement, duplicate
identities, and deterministic outputs are checked separately by a deterministic semantic validator.

The existing OpenRewrite message `renovatio.cobol.ir` remains authoritative for deterministic
translation. A validated wrapper may additionally be placed under
`renovatio.cobol.annotated-ir`. Recipes perform no loading, provider calls, credential lookup, prompt
selection, or network access. A missing, stale, or inconsistent wrapper leaves the legacy path
unchanged.

The published v1 contract is immutable. Any accepted-document change, identity-input change, new
family, new enum member, or semantic reinterpretation requires a new schema version.

## Consequences

- CI can validate committed sidecars and cache hits without provider credentials.
- Replaying the same canonical input and prompt version yields the same identity.
- Human review state can change without rewriting the underlying proposal identity.
- Provider orchestration must validate before context injection and report rejected sidecars.
- Schema validation and semantic validation remain two explicit gates.
- Supporting future annotation families requires a versioned schema and reader update.

## Rejected alternatives

- LLM calls inside OpenRewrite recipes: rejected because they make CI non-reproducible and couple
  AST changes to network availability.
- Embedding a copy of the base IR: rejected because it creates a second semantic authority and stale
  document risk.
- Default object serialization for hashes: rejected because serializer configuration and map order
  are not stable identity inputs.
- One hash for proposal, provider response, and review state: rejected because operational metadata
  would invalidate semantic cache identity.
