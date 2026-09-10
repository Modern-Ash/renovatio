# Implementation plan: LLM runtime, prompt catalog, and cache

> GitHub issue: [#125](https://github.com/Modern-Ash/renovatio/issues/125)
> Agora work: `ai-modernization/llm-runtime-catalog-cache`
> Authoritative spec: `docs/specs/llm-runtime-catalog-cache-v2.md`

## Delivery order

1. **Module boundary and service clarity**
   - Add `renovatio-llm` to the Maven reactor with Java 17 and a dependency on
     `renovatio-cobol-ir`.
   - Rename `LlmIntegrationService` and its tests to `CobolNqlParsingService`.
   - Verify IR and OpenRewrite recipe modules gain no provider or HTTP dependency.

2. **PromptCatalog contract**
   - Add strict YAML models/loaders, the five v1 prompt entries, schemas, fallback templates, and
     closed selector/validator registries.
   - Fail startup on unknown fields, duplicate/unversioned IDs, empty few-shot examples, missing
     resources, and unknown selectors/validators.
   - Test one valid catalog and every rejection class offline.

3. **Provider-neutral runtime**
   - Implement the provider interface, immutable request/response models, offline fake, and
     Anthropic Messages adapter.
   - Add configuration preflight, temperature-zero request policy, 60-second timeout, three-attempt
     retry policy, injected scheduler/random source, and sanitized errors.
   - Test retry classification and full-jitter bounds without sleeping or network access.

4. **Canonical cache and validation**
   - Reuse the accepted RFC 8785 canonicalizer and implement the complete cache identity contract.
   - Add strict envelopes, output validation pipeline, deterministic fallback, sanitization
     allowlist, quarantine, and committed-index/manifest verification.
   - Test identity invalidation for every component, zero provider calls on verified hits,
     ineligible working-tree candidates, and fail-closed mismatches.

5. **Agora attribution and promotion**
   - Declare `.agora/tools/llm-enrichment/TOOL.md` and implement `llm-enrichment/enrich` so the
     operation wraps the full cache-miss lifecycle.
   - Record only reviewed identifiers, hashes, dispositions, sanitized diagnostics, and artifact
     URIs.
   - Implement promotion checks for envelope Commit A, index Commit B, approval/evidence Commit C,
     and generated-manifest Commit D; reconcile the candidate against the durable completed Agora
     run/result and ensure runtime consumes only the build-verified manifest.
   - Test attribution initialization failures, observable in-process finalization failures,
     retrospective persistence absence/mismatch, and secret exclusion.

6. **Verification and handoff**
   - Run focused module/provider tests and the clean Java 17 Maven reactor.
   - Produce a test report mapping every acceptance scenario to deterministic evidence.
   - Run Agora consistency, traceability, readiness, review, and approval gates before completion.

## Guardrails

- No live provider request occurs in unit or reactor tests.
- No secret, authorization header, raw prompt, unrestricted IR, or provider envelope is persisted.
- Provider output is never accepted without schema and deterministic validators.
- Recipes never call an LLM and never receive invalid annotated context.
- Cache candidates are not lookup-eligible before governed promotion is complete.
- Each phase lands with focused tests; a failing phase blocks subsequent phases.

## Criterion mapping

| Criterion | Planned delivery | Primary verification |
| --- | --- | --- |
| `prompt-catalog` | Phase 2 | Strict loader and resource tests |
| `provider-wiring` | Phase 3 | Fake-provider, configuration, timeout, and retry tests |
| `cache` | Phase 4 | Canonical identity, hit/miss, index, and quarantine tests |
| `agora-attribution` | Phase 5 | Governed operation and redaction/failure tests |
| `service-clarity` | Phase 1 | Compile/tests and absence of misleading class name |

## Rollback

The new module is additive and has no recipe dependency. If runtime or cache integration fails,
disable its wiring and retain the deterministic COBOL lane. Cache candidates and quarantined files
can be omitted from promotion without changing base IR semantics.
