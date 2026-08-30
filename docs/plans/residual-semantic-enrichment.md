# Implementation plan: residual semantic enrichment

> GitHub issue: [#126](https://github.com/Modern-Ash/renovatio/issues/126)
> Agora work: `ai-modernization/residual-semantic-enrichment`
> Authoritative spec: `docs/specs/residual-semantic-enrichment.md`

## Delivery order

1. **Closed residual routing boundary**
   - Add a closed construction classification and request contract under
     `org.shark.renovatio.llm.residual` for domain naming, irreducible control flow, data intent,
     unsupported constructs, and the deterministic lane.
   - Route only the residual classes to the governed enrichment runtime. The deterministic class
     returns its existing result without preparing a prompt, reading the cache, or calling a
     provider.
   - Add spy-based tests proving zero runtime/provider interactions for MOVE, COMPUTE, IF,
     EVALUATE, simple PERFORM, basic PIC mapping, and level-88 conditions.

2. **Typed annotation assembly and provenance**
   - Convert validated catalog outputs into the existing `AnnotatedCobolModel` contract without
     weakening its identity, provenance, or validator rules.
   - Preserve prompt ID/version, source-node identity, cache identity, disposition, and review
     state in the sidecar; never mutate the base `CobolIntermediateModel`.
   - Reject outputs that cannot be mapped completely and emit the catalog's deterministic fallback
     plus a reviewable diagnostic.

3. **Domain naming suggestions**
   - Use `cobol.domain.naming.v1` only for paragraph/section/domain naming suggestions.
   - Validate identifiers, reserved words, stable collision scopes, and public-signature
     protection before an annotation is eligible for review.
   - Keep suggestions non-applying: acceptance records human intent in annotated IR and does not
     rename Java AST or COBOL IR directly.

4. **Irreducible `GO TO` restructuring plans**
   - Use `cobol.goto.restructure.v1` only for an irreducible control-flow classification.
   - Require the referenced characterization evidence to be present and green before retaining a
     proposed plan. Missing or red evidence discards the proposal, emits
     `LLM_CHARACTERIZATION_NOT_GREEN`, and selects deterministic transliteration plus a manual
     action.
   - Test reducible graphs stay deterministic and that proposals never alter executable code.

5. **Human-confirmed data intent and manual actions**
   - Map `REDEFINES` and `OCCURS DEPENDING ON` interpretations to pending annotations using their
     dedicated catalog prompts.
   - Add explicit accept/reject transitions restricted to the human spec-owner actor; neither the
     provider nor an automated project agent may confirm semantic intent.
   - Turn unsupported or rejected interpretations into stable, deduplicated manual action items
     with source location, reason, fallback, and provenance.

6. **Integration verification and governed evidence**
   - Add offline fixtures spanning all residual classes, mixed deterministic/residual programs,
     cache hit/miss behavior, invalid output, fallback, and human review transitions.
   - Run `mvn -pl renovatio-llm,renovatio-cobol-ir -am test`, then `mvn test` on Java 17.
   - Publish a test report mapping every acceptance criterion to tests and command results, then run
     Agora traceability, review, approval, and completion gates.

## Guardrails

- The LLM remains outside OpenRewrite recipes and never becomes the source of deterministic COBOL
  semantics.
- No proposal is auto-applied to COBOL IR, Java AST, or public signatures.
- Every residual call uses the governed runtime, versioned prompt catalog, content-addressed cache,
  schema validation, deterministic validators, and Agora attribution established by issue #125.
- Characterization evidence gates control-flow proposals; failure always preserves deterministic
  transliteration and creates a manual action.
- Only a human spec owner can confirm ambiguous data-layout intent.
- Tests are offline and make no live provider request.

## Criterion mapping

| Criterion | Planned delivery | Primary verification |
| --- | --- | --- |
| `domain-language` | Phases 2 and 3 | Identifier/collision/signature tests and typed sidecar fixture |
| `goto-plan` | Phase 4 | Irreducible/reducible CFG and green/red/missing characterization tests |
| `human-confirmation` | Phase 5 | Actor authorization and accept/reject transition tests |
| `manual-actions` | Phases 4 and 5 | Stable fallback/action-item snapshot tests |
| `residual-only` | Phase 1 | Zero prompt/cache/provider interaction tests for deterministic constructs |

## Planned files

- `renovatio-llm/src/main/java/org/shark/renovatio/llm/residual/**`
- `renovatio-llm/src/test/java/org/shark/renovatio/llm/residual/**`
- Existing annotated-IR types under
  `renovatio-cobol-ir/src/main/java/org/shark/renovatio/cobol/ir/annotated/**` only where the
  accepted v1 contract requires an additive typed field or validator.
- `docs/testing/residual-semantic-enrichment.md` for final evidence.

## Rollback

The residual coordinator is additive and sits before the already governed runtime. Disabling its
wiring leaves the deterministic parser/recipe path unchanged. Rejected or invalid annotations can
be removed without changing the base IR, generated code, or committed characterization fixtures.
