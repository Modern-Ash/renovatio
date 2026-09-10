# Implementation Plan: Versioned annotated COBOL IR sidecar

> GitHub issue: [#124](https://github.com/Modern-Ash/renovatio/issues/124)
> Agora work: `ai-modernization/annotated-ir-contract`
> Specification: `docs/specs/annotated-ir-contract.md`

## 1. Outcome

Deliver the immutable annotated sidecar model, strict v1 schema, RFC 8785-based content identities,
semantic validation, and backward-compatible OpenRewrite context seam specified for #124. No
provider, prompt, network, or enrichment execution is introduced.

## 2. Delivery sequence

### Step 1 — Typed sidecar model

- Add `AnnotatedCobolModel`, `AnnotatedCobolContext`, annotation envelope, provenance, review state,
  closed node kinds, and the four typed payload families to `renovatio-cobol-ir`.
- Enforce defensive copies and review-state snapshot invariants at construction.
- Permit empty annotations in memory while keeping persisted enriched sidecars nonempty.

### Step 2 — Canonical identity

- Add the dedicated `cobol-ir.v1` projector and identity-bearing node enumerator.
- Implement RFC 6901 paths, exhaustive node-kind mapping, RFC 8785 canonicalization, domain-separated
  annotation/cache projections, and lowercase SHA-256.
- Add golden vectors for key ordering, absent fields, duplicate text at different paths, and input
  invalidation.

### Step 3 — Strict persisted schema

- Add `cobol-annotated-ir.v1.schema.json` with closed objects, discriminated typed payloads,
  conditional review fields, formats, enums, confidence bounds, and `minItems: 1`.
- Add valid and invalid committed fixtures following #122 conventions.
- Register the schema as the governed `json-schema` artifact.

### Step 4 — Semantic validation

- Verify base hash, node resolution/kind, affected control-flow nodes, unique annotation IDs,
  deterministic output conflicts, and supported versions.
- Emit the six specified codes with stable JSON Pointers and deterministic ordering.
- Keep schema validation and cross-document semantic validation separate.

### Step 5 — ExecutionContext compatibility

- Preserve `renovatio.cobol.ir` for the base model.
- Add `renovatio.cobol.annotated-ir` for a validated `AnnotatedCobolContext`.
- Prove invalid/missing/stale sidecars are omitted while deterministic legacy execution continues.
- Add an architectural test forbidding provider/network dependencies from the IR and recipe seam.

### Step 6 — ADR and verification

- Record canonicalization, domain separation, schema immutability, and compatibility decisions in an
  ADR and register it as `architecture-decision-record`.
- Run Java 17 tests for IR, recipes, and provider modules; validate positive/negative fixtures and
  repeat identity generation to prove byte stability.
- Register the successful test report before verification/completion transitions.

## 3. Acceptance mapping

| Criterion | Planned coverage |
| --- | --- |
| `model` | Steps 1 and 5 implement immutable sidecar/context types without base mutation. |
| `sidecar-schema` | Steps 3 and 4 enforce strict shape and semantic references. |
| `content-identity` | Step 2 implements all normative projections and stable hashes. |
| `context-seam` | Step 5 preserves legacy behavior and adds the validated wrapper key. |

## 4. Verification commands

```bash
mvn -B -pl renovatio-cobol-ir,cobol-openrewrite-recipes,renovatio-provider-cobol -am test
mvn -B -pl renovatio-cobol-ir -am clean test
```

Final verification also validates committed sidecar fixtures and compares identity outputs from two
independent runs.

## 5. Risks and controls

- Canonicalization drift: golden RFC 8785 vectors and no default serializer in identity paths.
- Schema/model divergence: fixtures validate both Java construction and JSON Schema.
- Accidental semantic authority: sidecar never embeds or mutates base IR.
- Stale annotations: base hash and node resolution fail closed before annotated context injection.
- Recipe/provider coupling: dependency tests enforce the offline seam.
