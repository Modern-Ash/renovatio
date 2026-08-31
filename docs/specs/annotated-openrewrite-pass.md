# Specification: Deterministic OpenRewrite pass over annotated IR

> GitHub issue: [#127](https://github.com/Modern-Ash/renovatio/issues/127)
> Agora work: `ai-modernization/annotated-openrewrite-pass`

## 1. Outcome and boundary

Activate deterministic consumption of a validated `AnnotatedCobolModel` sidecar inside the existing
OpenRewrite translation pass. When an accepted annotation matches the current base IR, the pass
applies it to the generated Java through AST-safe transformations only. Every other case &mdash;
missing, pending, rejected, stale, colliding, or belonging to a non-applied family &mdash; falls back
to the deterministic base translation and emits a traceable manual action item.

This specification is authoritative for issue #127. It preserves the contracts of issues #122
(characterization harness and strict schemas), #124 (`cobol-annotated-ir.v1` sidecar and the
`ExecutionContext` seam), and #126 (residual enrichment routing and provenance). It never introduces
a provider client, credential, network call, or prompt dependency into the recipe or IR modules.

## 2. Authoritative dependencies

| Dependency | Required contract |
| --- | --- |
| Annotated IR | `docs/specs/annotated-ir-contract.md`, schema `cobol-annotated-ir.v1`, `renovatio-cobol-ir/src/main/resources/schema/cobol-annotated-ir.v1.schema.json`, and the immutable `AnnotatedCobolContext` transport pair. |
| Execution seam | `PopulateCobolProcessRecipe.CONTEXT_KEY` (`renovatio.cobol.ir`) and `AnnotatedCobolContext.CONTEXT_KEY` (`renovatio.cobol.annotated-ir`). |
| Identity | `CobolIrIdentityProjector` for `baseIrHash` and per-node `nodeId`; base IR version `cobol-ir.v1`. |
| Manual action items | `manual-action-item.v1` schema and the existing `ManualActionItem` / `ManualActionItemWriter` in `renovatio-provider-cobol`. |
| Characterization harness | `docs/specs/characterization-guardrails.md` and `CharacterizationFixtureContractTest`; annotated fixtures run offline through the same lane. |

Unsupported or mismatched sidecar versions fail closed to deterministic translation plus a manual
action item. No best-effort conversion between contract versions is permitted.

## 3. Applied families (v1)

The deterministic pass mutates the generated AST for exactly two annotation families. The other two
never mutate program logic or structure; they are recorded as manual action items.

| Family | v1 disposition |
| --- | --- |
| `DOMAIN_NAMING` | Applied. Renames the Java identifier bound to the annotated node. |
| `DATA_INTENT` | Applied. Attaches a `@CobolDataIntent` marker annotation to the affected field or class. |
| `CONTROL_FLOW_PLAN` | Not applied. Emits a manual action item carrying the ordered plan and risks. |
| `UNSUPPORTED_EXPLANATION` | Not applied. Emits a manual action item carrying the explanation and manual action. |

### 3.1 Eligibility

An annotation is eligible for application only when all hold:

- `sidecar.baseIrHash` equals `CobolIrIdentityProjector.baseIrHash(model)` for the in-memory base
  model being translated;
- `sidecar.schemaVersion` is exactly `cobol-annotated-ir.v1` and the sidecar passes
  `AnnotatedCobolValidator` with zero diagnostics;
- `annotation.review.reviewState` is exactly `ACCEPTED`;
- `annotation.nodeId` resolves through `CobolIrIdentityProjector` to exactly one identity-bearing
  node present in the base model, and `annotation.nodeKind` agrees with that node.

Any annotation failing any condition is dropped from application. Dropped annotations in states
`REJECTED`, `PROPOSED`, or `NEEDS_REVIEW`, and any annotation dropped because of a stale
`baseIrHash`, produce a manual action item (section 6). A structurally invalid sidecar is treated as
absent: the pass runs the deterministic legacy lane and orchestration records the validator
diagnostic.

### 3.2 `DOMAIN_NAMING` application

- The applicator maps `nodeId` to the current Java identifier by projecting the base node's RFC 6901
  pointer to its COBOL name and deriving the generated name with the same casing rules the recipe
  already uses (`toPascal` for types/accessor stems, camel case for fields and locals).
- Target identifiers for a data-item node: the private DTO field, its getter, its setter, and every
  read/write use within the generated compilation unit. For a paragraph node mapped to a service
  method: the method name and its internal uses.
- Renames use OpenRewrite AST operations (`RenameVariable`, `ChangeFieldName`, `ChangeMethodName`,
  or `JavaTemplate`-based rewrites). Raw text edits are forbidden.
- Public signatures are renamed. The generated output has no external consumers at this stage.
- Collision: if the `suggestedName` (normalized to a valid Java identifier) already denotes a
  different field, method, parameter, local, or type in the same scope, the annotation is not
  applied and a manual action item is emitted.
- `boundedContext` and `rationale` do not change code in v1; `rationale` is carried into the
  provenance of any related action item.

### 3.3 `DATA_INTENT` application

- The applicator attaches `@CobolDataIntent` to the `J.VariableDeclarations` for a data-item node,
  or to the `J.ClassDeclaration` when the node denotes an aggregate mapped to a class.
- The annotation is informational. It changes no field type, initializer, accessor, or control flow.
- Payload mapping: `construction` &rarr; `construction()`, `interpretation` &rarr; `interpretation()`,
  `assumptions` &rarr; `assumptions()`, plus `annotationId()` for traceability and `nodeId()`.

## 4. `@CobolDataIntent` annotation type

A new Maven module `renovatio-cobol-annotations` publishes exactly one public type and no runtime
dependencies:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface CobolDataIntent {
    String nodeId();
    String annotationId();
    Construction construction();        // REDEFINES | OCCURS_DEPENDING_ON
    String interpretation();
    String[] assumptions();
    enum Construction { REDEFINES, OCCURS_DEPENDING_ON }
}
```

`cobol-openrewrite-recipes` depends on this module at compile scope. `JavaGenerationService` adds it
to the classpath of generated stubs so the emitted sources compile. The module contains no provider,
network, or IO code and is covered by the recipe-boundary architecture test.

## 5. Context resolution and data flow

`JavaGenerationService` stops calling the base-model-only overload unconditionally. A new
`AnnotatedContextResolver` in `renovatio-provider-cobol` resolves the context with fixed precedence:

1. **Request sidecar** &mdash; when the migration request carries an `AnnotatedCobolModel` (or a
   path to one), validate it against the in-memory base model and use it.
2. **Committed path sidecar** &mdash; otherwise, look for `<program>.annotated.json` beside the
   COBOL source; if present and valid, use it.
3. **Legacy** &mdash; otherwise resolve to `null` and translate with the base model only.

Resolution is deterministic and offline. It performs schema validation, `AnnotatedCobolValidator`,
and the `baseIrHash` match before constructing an `AnnotatedCobolContext`. An invalid or stale
sidecar never becomes a context; it degrades to the next precedence level and records a diagnostic
or manual action item.

`CobolSemanticTranspiler` places the validated `AnnotatedCobolContext` under
`AnnotatedCobolContext.CONTEXT_KEY`, keeps the base model under `PopulateCobolProcessRecipe.CONTEXT_KEY`,
runs `PopulateCobolProcessRecipe`, then drains accumulated manual action items from the execution
context and hands them to `ManualActionItemWriter`.

`PopulateCobolProcessRecipe` first renders the deterministic body exactly as today. A new
`AnnotationApplicator` then runs as a post-processing visitor over the resulting compilation unit:
it reads `sidecar.annotations()`, filters by eligibility (section 3.1), orders survivors by
`(nodeId, annotationId)`, and applies each. Ineligible or dropped annotations are appended to the
execution-context action-item list.

## 6. Fallback and manual action items

Every emitted item conforms to `manual-action-item.v1`. Item field conventions:

| Trigger | `failedGate` | `reason` |
| --- | --- | --- |
| Annotation `REJECTED` in sidecar | `review-eligibility` | Rejected annotation not applied; deterministic translation retained. |
| Annotation `PROPOSED` or `NEEDS_REVIEW` | `review-eligibility` | Annotation pending human review; not eligible for deterministic application. |
| Sidecar `baseIrHash` mismatch (stale) | `characterization` | Annotated sidecar does not match current IR; regenerate the sidecar. |
| `DOMAIN_NAMING` collision | `review-eligibility` | Domain rename collides with an existing identifier in scope. |
| `DOMAIN_NAMING`/`DATA_INTENT` node unresolved or kind mismatch | `review-eligibility` | Annotation node cannot be resolved against the current IR. |
| `CONTROL_FLOW_PLAN` present | `review-eligibility` | Control-flow plan requires reviewed restructuring; not applied deterministically. |
| `UNSUPPORTED_EXPLANATION` present | `review-eligibility` | Unsupported construct explanation recorded for manual action. |

`requiredHumanAction` and `acceptanceCondition` restate the annotation payload content.
`diagnosticReference` is a stable code per trigger. `severity` is `error` for stale sidecars and
unresolved nodes, `warning` otherwise. The generated Java is always the deterministic translation,
optionally with eligible renames and markers applied; it is never left partially transformed.

## 7. Purity and determinism

- `AnnotationApplicator` and every helper live in `cobol-openrewrite-recipes` and import nothing
  from `org.shark.renovatio.provider.*`, no HTTP client, no credential resolver, and no prompt
  catalog. The existing recipe-boundary architecture test and Maven Enforcer rule are extended to
  assert this for the new code and the `renovatio-cobol-annotations` dependency.
- Applying the same base model and the same validated sidecar twice produces byte-identical Java.
  Annotation ordering is fully determined by `(nodeId, annotationId)`; execution never reads wall
  clock, environment, random sources, or map iteration order.
- No network access or provider credential is required to build, test, or run the pass in CI.

## 8. Acceptance scenarios

### 8.1 annotated-consumption

- `CobolSemanticTranspiler` injects a validated `AnnotatedCobolContext`; `PopulateCobolProcessRecipe`
  reads `sidecar.annotations()` through `AnnotatedCobolContext.CONTEXT_KEY`.
- `AnnotatedContextResolver` selects request sidecar over committed path over legacy, and rejects an
  invalid sidecar without failing the translation.
- An accepted `DOMAIN_NAMING` annotation renames the bound field and its accessors in the generated
  source; an accepted `DATA_INTENT` annotation adds `@CobolDataIntent` to the field.

### 8.2 ast-safe

- Only `ACCEPTED` annotations whose node resolves and whose kind agrees are applied.
- All mutations go through OpenRewrite AST operations; a test asserts the applicator performs no raw
  string replacement on source text.
- Applying an annotation to an unrelated compilation unit is a no-op.

### 8.3 no-provider-call

- Architecture tests prove no provider, HTTP, credential, or prompt type is reachable from
  `AnnotationApplicator` or `renovatio-cobol-annotations`.
- The pass runs with deliberately failing provider and attribution suppliers and performs zero
  calls.

### 8.4 reproducible

- A characterization fixture pairs a committed `<fixture>.annotated.json` with an
  `expected-annotated.java` golden.
- `CharacterizationFixtureContractTest` generates the annotated output twice from the same inputs
  and asserts both runs are byte-identical to each other and to the golden, offline.

### 8.5 fallback

- `REJECTED`, `PROPOSED`, `NEEDS_REVIEW`, stale, collision, unresolved-node, `CONTROL_FLOW_PLAN`,
  and `UNSUPPORTED_EXPLANATION` each produce exactly one schema-valid `manual-action-item.v1` entry
  with the codes in section 6.
- In every fallback case the generated Java equals the deterministic base translation (plus any
  independently eligible annotations) and compiles.

## 9. Construct-to-test matrix

| Construct / path | Unit test | Characterization fixture |
| --- | --- | --- |
| `DOMAIN_NAMING` field rename | `AnnotationApplicatorTest` | annotated `move-numeric` |
| `DOMAIN_NAMING` collision drop + item | `AnnotationApplicatorTest` | &mdash; |
| `DATA_INTENT` marker on field | `AnnotationApplicatorTest` | annotated `redefines`/`odo` fixture |
| `REJECTED` / `PROPOSED` / `NEEDS_REVIEW` drop + item | `AnnotationApplicatorTest` | &mdash; |
| stale `baseIrHash` drop + item | `AnnotationApplicatorTest`, `AnnotatedContextResolverTest` | &mdash; |
| `CONTROL_FLOW_PLAN` / `UNSUPPORTED_EXPLANATION` item | `AnnotationApplicatorTest` | &mdash; |
| request &gt; path &gt; legacy precedence | `AnnotatedContextResolverTest` | &mdash; |
| deterministic double-run equality | `AnnotationApplicatorTest` | `CharacterizationFixtureContractTest` |
| recipe purity | recipe-boundary architecture test | &mdash; |

## 10. Delivery artifacts

- this specification, before transition to `clarified`;
- an implementation plan, before implementation gates;
- the `renovatio-cobol-annotations` module and `@CobolDataIntent` type;
- `AnnotationApplicator` in `cobol-openrewrite-recipes` and `AnnotatedContextResolver` in
  `renovatio-provider-cobol`, wired through `JavaGenerationService` and `CobolSemanticTranspiler`;
- annotated characterization fixtures wired into the offline lane;
- a successful test report, before completion.

## 11. Out of scope

- Generating annotations, calling any provider, prompt execution, or cache storage (issues
  #125&ndash;#126).
- Applying `CONTROL_FLOW_PLAN` restructuring or `UNSUPPORTED_EXPLANATION` code changes.
- Review-state transitions or review history (issue #124 validates snapshots only).
- Idiomatic polish beyond the two applied families (issue #128).
