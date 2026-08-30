# Specification: Versioned annotated COBOL IR sidecar contract

> GitHub issue: [#124](https://github.com/Modern-Ash/renovatio/issues/124)
> Agora work: `ai-modernization/annotated-ir-contract`

## 1. Outcome

Define a strict, additive, versioned `AnnotatedCobolModel` sidecar over
`CobolIntermediateModel`. The sidecar carries reviewable semantic suggestions and their provenance
without copying, replacing, or mutating base semantic nodes. A committed sidecar is reproducible,
content-addressed, auditable through Agora, and safe to transport through the existing OpenRewrite
`ExecutionContext` seam.

## 2. Scope

This work specifies and implements:

- an immutable Java model for an annotated document and typed annotations;
- canonical identity for a base IR document, individual semantic nodes, annotations, and cache keys;
- provider/model/prompt/tool-run provenance;
- confidence, validation, and human review state;
- a strict versioned JSON Schema for `*.annotated.json` files;
- validation that rejects unknown fields, malformed identities, duplicate annotations, and references
  to nodes absent from the supplied base IR;
- transport of a validated annotated model through the existing `ExecutionContext` boundary.

It does not implement provider calls, prompt execution, cache storage, semantic enrichment,
OpenRewrite transformations, or automatic application of annotations. Those belong to issues
#125–#127.

## 3. Additive model contract

`AnnotatedCobolModel` is a sidecar envelope with these required logical fields:

| Field | Contract |
| --- | --- |
| `schemaVersion` | Exact sidecar schema identifier, initially `cobol-annotated-ir.v1`. |
| `baseIrVersion` | Exact schema version of the referenced base IR. |
| `baseIrHash` | Lowercase SHA-256 identity of the canonical complete base IR. |
| `annotations` | Ordered annotation array; the base IR is not embedded. |

Every annotation contains:

- `annotationId`, derived from its canonical identity inputs;
- `nodeId`, which must resolve to exactly one node in the supplied base IR;
- `nodeKind`, used as a type discriminator and cross-check against the base node;
- an annotation-family discriminator and a schema-valid typed payload;
- `confidence`, inclusive from `0.0` through `1.0`;
- provenance and review records defined below.

The v1 annotation families and payloads are:

| Family | Required typed payload |
| --- | --- |
| `DOMAIN_NAMING` | Nonblank `suggestedName` and `rationale`; optional nonblank `boundedContext`. |
| `CONTROL_FLOW_PLAN` | Nonempty `affectedNodeIds`, ordered nonempty `steps`, and nonempty `risks`. |
| `DATA_INTENT` | `construction` (`REDEFINES` or `OCCURS_DEPENDING_ON`), nonblank `interpretation`, and nonempty `assumptions`. |
| `UNSUPPORTED_EXPLANATION` | Nonblank `construction`, `explanation`, and `manualAction`. |

Construction performs defensive copies. No API exposes a mutation path to the base model or the
annotation collections. The sidecar references the base IR by version, document hash, and node IDs;
it never becomes a second semantic authority.

`AnnotatedCobolModel` does not retain a `CobolIntermediateModel` reference. The separate immutable
`AnnotatedCobolContext` pairs the exact in-memory base-model reference with a validated sidecar for
transport only.

## 4. Canonical identity

All identities use SHA-256 encoded as exactly 64 lowercase hexadecimal characters. Hash inputs use
RFC 8785 JSON Canonicalization Scheme over UTF-8 JSON. Numbers must be finite canonical JSON
numbers; NaN and infinities are rejected. Java enums serialize by exact name, absent optionals stay
absent, and Java object identity or map iteration order never enters a hash.

The authoritative base serialization is schema version `cobol-ir.v1` and
`renovatio-cobol-ir/src/main/resources/schema/cobol-ir.v1.schema.json`. A dedicated IR canonicalizer
produces schema-valid projections; arbitrary default Jackson serialization is not an identity input.

### 4.1 Base document and node identity

- `baseIrHash = sha256(canonical(baseIr))`.
- `nodeId = sha256(canonical(nodeIdentityProjection))`.
- The node identity projection includes the base IR schema version, node kind, stable RFC 6901 JSON
  Pointer from the canonical IR root, source span when present, and semantic node content.
- Annotation data, timestamps, provider responses, and generated Java are excluded from node IDs.
- Two nodes with identical text at different structural paths remain distinct.

Normatively, `nodeIdentityProjection` is an RFC 8785 canonical object with required fields
`baseIrVersion`, `nodeKind`, `path`, and `semanticContent`, plus `sourceSpan` only when present.
`semanticContent` is the node serialized according to the base IR schema with annotations and
derived identities excluded. Enums use their exact names, absent optional fields are omitted, and
arrays preserve source order.

Identity-bearing nodes are data items, their level-88 conditions and values, paragraphs, recursively
nested statements, and typed expressions/conditions. Document metadata, execution context,
diagnostics, and derived control-flow views are not annotation targets. Arrays use source-order
indices. Maps use RFC 6901-escaped semantic keys and are enumerated in lexicographic key order; the
pointer addresses the key itself and therefore does not depend on map iteration order.

The closed `nodeKind` set is `DATA_ITEM`, `LEVEL_88_CONDITION`, `LEVEL_88_VALUE`, `PARAGRAPH`,
`MOVE_STATEMENT`, `COMPUTE_STATEMENT`, `IF_STATEMENT`, `EVALUATE_STATEMENT`, `EVALUATE_BRANCH`,
`PERFORM_STATEMENT`, `CALL_STATEMENT`, `DB2_STATEMENT`, `FILE_OPERATION_STATEMENT`,
`LITERAL_EXPRESSION`, `DATA_REFERENCE_EXPRESSION`, `UNARY_ARITHMETIC_EXPRESSION`,
`BINARY_ARITHMETIC_EXPRESSION`, `COMPARISON_CONDITION`, `BOOLEAN_CONDITION`, `NEGATED_CONDITION`, and
`LEVEL_88_CONDITION_REFERENCE`. The canonicalizer owns an exhaustive Java-type/schema-definition
mapping and fails closed on an unmapped identity-bearing type.

### 4.2 Annotation and cache identity

- `annotationId = sha256(nodeId + annotationFamily + promptId + promptVersion + outputSchemaVersion + inputHash)`.
- The downstream cache key additionally binds the canonical enrichment input hash.
- Changing node content, prompt version, annotation family, or output schema version invalidates the
  identity. Changing review metadata does not rewrite the underlying proposal identity.

Hash concatenation uses an unambiguous canonical JSON array, not raw string concatenation.

For one complete annotation identity, a different validated output is a nondeterminism conflict and
is rejected rather than stored as a competing proposal. Payload, confidence, provider model, review
state, and `outputHash` do not enter `annotationId`.

Normatively, the cache-key projection is the RFC 8785 object with exactly `nodeId`,
`annotationFamily`, `promptId`, `promptVersion`, `outputSchemaVersion`, and `inputHash`. Its lowercase
SHA-256 digest is the cache key. Timestamps, provider model, review state, cache disposition, and
tool-run identity are excluded.

## 5. Provenance and review

Provenance is required for every annotation and contains:

- `provider`, `model`, `promptId`, `promptVersion`, and `outputSchemaVersion`;
- `inputHash` and `outputHash`;
- `toolRunRef`, matching `^tool-[0-9]{8}t[0-9]{14}z$`;
- `cacheDisposition`, either `HIT` or `MISS`.

Secrets, raw credentials, authorization headers, and unrestricted prompts/responses are forbidden.
Prompt and response content belongs in separately governed artifacts addressed by hash.

`outputHash` identifies the validated typed proposal projection containing exactly
`annotationFamily`, `payload`, and `confidence`. It excludes raw provider envelopes/text, annotation
and node IDs, provenance, review metadata, timestamps, and cache disposition.

`reviewState` is exactly one of:

- `PROPOSED`: generated and not yet assigned for human review;
- `NEEDS_REVIEW`: explicitly queued for a human decision;
- `ACCEPTED`: approved by a human reviewer;
- `REJECTED`: declined by a human reviewer.

`ACCEPTED` and `REJECTED` require nonblank `reviewedBy` and an RFC 3339 UTC `reviewedAt` timestamp.
`PROPOSED` must not contain `assignedReviewer`, `reviewedBy`, or `reviewedAt`. `NEEDS_REVIEW` may
contain an optional nonblank `assignedReviewer`, but `reviewedBy` and `reviewedAt` must be absent.
Provider or agent execution alone cannot create an accepted state.

Version 1 validates these review-state snapshot invariants only. It does not encode or infer review
history or authorize transitions between states.

## 6. Strict sidecar schema

The schema uses JSON Schema 2020-12 and must:

- set `additionalProperties: false` for every owned object;
- require every discriminator, identity, provenance, confidence, and review field;
- constrain enums and hash/tool-run formats;
- require at least one annotation for a persisted enriched sidecar;
- leave cross-item duplicate `annotationId` rejection to the deterministic semantic validator;
- express conditional review-field requirements;
- reject malformed annotation payloads through family-specific definitions.

JSON Schema validates document shape. A deterministic semantic validator additionally verifies
hashes, unique IDs, base-document identity, node resolution, and node-kind agreement. Validation
returns stable diagnostics ordered by JSON pointer and diagnostic code.

Schema evolution is additive within a version. Removing/renaming a field, changing identity inputs,
or changing payload meaning requires a new schema version. Readers reject unknown major versions;
no best-effort interpretation is permitted.

## 7. ExecutionContext seam

The current provider continues placing `CobolIntermediateModel` under
`PopulateCobolProcessRecipe.CONTEXT_KEY` (`renovatio.cobol.ir`). The annotated contract preserves
that legacy value and adds `renovatio.cobol.annotated-ir` for a validated context value containing:

- the original immutable `CobolIntermediateModel`; and
- its validated `AnnotatedCobolModel` sidecar.

The provider performs loading and validation before recipe execution. Recipes only read the context
value. The sidecar model and IR modules must not depend on provider SDKs, HTTP clients, credential
resolution, prompt catalogs, or network APIs. Missing, stale, or invalid sidecars are not placed in
the recipe context.

## 8. Failure behavior

The contract fails closed:

- base hash mismatch: reject the complete sidecar;
- unresolved or kind-mismatched node: reject that document;
- unresolved `CONTROL_FLOW_PLAN.affectedNodeIds`: reject with stable diagnostics ordered by
  annotation JSON Pointer and affected-node array index;
- malformed/unknown annotation family: schema rejection;
- duplicate annotation identity: semantic validation rejection;
- invalid confidence or review-state snapshot: rejection;
- unsupported schema version: rejection with a stable diagnostic.

Validation never mutates the base IR, repairs an annotation silently, calls an LLM, or selects a
semantic fallback. Downstream orchestration owns deterministic fallback and manual action items.

## 9. Acceptance scenarios

### 9.1 Model

- Constructing a context preserves the exact base-model reference and immutable sidecar values.
- Serializing the sidecar does not embed or alter base semantic nodes.
- Annotation families use typed payloads rather than unstructured maps.

### 9.2 Schema

- A valid committed `*.annotated.json` fixture passes.
- Unknown properties, malformed hashes, invalid enums/confidence, and inconsistent review fields fail.
- Annotation payloads not matching their discriminator fail.

### 9.3 Identity

- Identical canonical inputs produce identical node, annotation, and cache hashes across runs.
- Key ordering and insignificant JSON whitespace do not change hashes.
- Semantic node, prompt-version, or output-schema changes do change the appropriate identity.
- Duplicate textual nodes at different paths receive distinct IDs.

### 9.4 Context seam

- A validated base/sidecar pair round-trips through `ExecutionContext`.
- A stale base hash is rejected before recipe invocation.
- Architectural tests prove no provider/network dependency enters IR or recipe modules.

## 10. Dependencies and delivery artifacts

The fixture and strict-schema conventions from #122 are available through commit `bbd35be` and PR
#129 on `main`. #123 is not a hard dependency because this contract binds explicit base IR versions
and hashes.

Required governed artifacts are:

- this specification before transition to `clarified`;
- the strict JSON Schema during implementation;
- an ADR documenting canonicalization, identity boundaries, and schema evolution before completion;
- an implementation plan and successful test report required by subsequent lifecycle gates.
