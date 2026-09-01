# Specification: Governed three-pass COBOL modernization

**Branch:** `agora/issue-128-idiomatic-polish-proposals`  
**Status:** Draft  
**Objective:** deliver a deterministic, reproducible, and auditable COBOL modernization path where core semantic translation is offline and LLM enrichment is governed, cached, and reviewable.

## Scope

We modernize COBOL programs through three bounded passes:

1. Deterministic semantic pass (OpenRewrite + IR runtime)
2. Governed enrichment pass (LLM + versioned annotation sidecar)
3. Review-only idiomatic polish pass (human-approved diffs)

The resulting Java output must remain behaviorally aligned with characterized COBOL fixtures and preserve known residual/manual actionability.

## Context and constraints

- Core translation behavior is owned by deterministic code paths only (parsers, IR, recipes, runtime).
- LLM calls are prohibited in deterministic components and must be attributed as Agora tool runs when residual.
- All outputs must be deterministic for a fixed source/version input set.
- Any unresolved or non-translatable areas are emitted as actionable `manual-action-items`.

## Acceptance criteria

- **deterministic-boundary:** Core semantic translation is pure, repeatable, offline, cacheable, and without LLM calls.
- **annotated-ir:** LLM enrichment emits a strict versioned annotated-IR sidecar with provenance and content-addressed identity.
- **governed-residual:** Explicit residual tasks only are routed through LLM paths; each uncached call is attributed as Agora tool-run.
- **guardrails:** Every LLM result passes schema validation, compilation checks, characterization tests, and review gates or creates deterministic fallbacks/manual action items.
- **review-only-polish:** Idiomatic refactors after transliteration are proposal-only and are not auto-applied.

## Functional requirements

- Preserve existing deterministic AST and semantic mapping behavior from completed sub-work items.
- Persist deterministic and versioned artifacts in repo-backed paths and generate deterministic fallback paths for enrichment failures.
- Emit manual action items that include original source path for precise remediation references.
- Ensure review-only polish can be regenerated without reapplying unsafe semantic edits.

## Deliverables

- `specs/ai-modernization/spec.md` (this document)
- `specs/ai-modernization/implementation-plan.md`
- `specs/ai-modernization/ADR-001-three-pass-llm-architecture.md` (existing)
- Existing child-work artifacts for deterministic tests, runtime/catalog and enrichment.

## Out of scope

- New language targets.
- External runtime calls embedded inside rewrite recipes.
- Non-governed LLM behavior outside residual workflow.

## Completion evidence

Completion is demonstrated when the parent work can be clarified and planned with explicit criterion coverage and every child work item’s artifacts/reports already accepted by Agora.
