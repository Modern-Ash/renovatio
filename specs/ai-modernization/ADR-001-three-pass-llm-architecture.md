# ADR-001: Three-pass deterministic and LLM-assisted COBOL modernization

- Status: Proposed
- Agora swarm: `ai-modernization`
- Parent work: `ai-modernization/three-pass-modernization`

## Context

Renovatio needs semantic fidelity and reproducibility for COBOL modernization while still using an
LLM where intent cannot be recovered safely from syntax alone. Embedding provider calls in
OpenRewrite recipes would make CI nondeterministic, network-dependent, difficult to cache, and hard
to audit.

The existing `LlmIntegrationService` is not an LLM client; it parses NQL. The actual provider wiring,
prompt catalog, cache, and governance integration do not exist yet.

## Decision

Use a three-pass architecture and keep the LLM on the residual path only. The deterministic parser,
IR, runtime, and recipes remain the semantic authority for roughly eighty percent of transformations.
LLM output is advisory and never the sole source of program semantics.

### Work partition

| Capability | Owner |
| --- | --- |
| `MOVE`, `COMPUTE`, `IF`, `EVALUATE`, simple `PERFORM`, basic PIC mapping, level-88 mapping | Deterministic parser, IR, runtime, and recipes |
| Domain names and bounded-context suggestions | LLM, reviewable annotation |
| Irreducible `GO TO` restructuring | LLM proposal validated by characterization tests |
| `REDEFINES` and `OCCURS DEPENDING ON` intent | LLM suggestion with explicit human confirmation |
| Unsupported-construction explanations and manual action items | LLM with deterministic fallback |
| Idiomatic post-transliteration refactors | LLM-proposed diff, never auto-applied |

### Pass A: offline enrichment over IR

Create a versioned `AnnotatedCobolModel` sidecar (`*.annotated.json`) over
`CobolIntermediateModel`. Annotations include stable node identity, prompt and model provenance,
confidence, validation state, human decision state, and fallback metadata.

Provider calls use temperature zero. Cache identity is derived from canonical IR-node content plus
the prompt version. Cache entries are committed, so cache hits and all deterministic lanes run
offline in CI. Every cache miss is attributable through an Agora tool-run without storing secrets.

### Pass B: deterministic OpenRewrite recipes

`CobolSemanticTranspiler` injects the validated annotated model through `ExecutionContext`.
`PopulateCobolProcessRecipe` and later recipes consume annotations through that seam. Recipes contain
no provider client, credential lookup, prompt rendering, or network call and apply only AST-safe,
schema-approved edits.

### Pass C: optional idiomatic polish

The LLM may propose a patch after deterministic transliteration passes all gates. The patch is a
review artifact only. It is discarded on any validation failure and never has an automatic apply
path.

## Guardrails

Every LLM result must pass, in order:

1. strict output-schema validation;
2. compilation of the generated target;
3. green characterization tests;
4. human review when semantics or source changes are proposed.

Failure at any stage selects deterministic transliteration and records a manual action item. The LLM
never overrides the base IR or becomes the only representation of semantic intent.

## Prompt catalog contract

The new `renovatio-llm` module owns versioned YAML entries with this logical shape:

```yaml
promptId: cobol.goto.restructure.v3
appliesTo: irreducible-control-flow
system: "..."
fewShot: []
outputSchema: schemas/goto-restructure-v3.json
validators:
  - schema
  - compile
  - characterization
fallback: deterministic-transliteration-with-action-item
```

Provider integration is hidden behind an interface. The first real adapter targets Claude and uses
environment-based credentials, bounded timeouts, retry policy, and an offline fake for tests.

## Delivery queue

1. `characterization-guardrails`
2. `deterministic-semantic-core` and `annotated-ir-contract` in parallel after fixture conventions
3. `llm-runtime-catalog-cache`
4. `residual-semantic-enrichment`
5. `annotated-openrewrite-pass`
6. `idiomatic-polish-proposals` as an optional final slice

The Agora child work descriptions are the durable dependency declarations until a first-class work
dependency relation is introduced.

## Consequences

- Deterministic transformations remain reproducible, cacheable, and suitable for CI.
- Provider cost and nondeterminism are limited to explicit residual cases.
- Prompt and model changes invalidate cache entries through content identity rather than hidden
  mutable state.
- Advanced COBOL intent remains reviewable and can fall back without blocking basic transliteration.
- More artifacts are committed, but they provide the audit trail required for governed migration.
