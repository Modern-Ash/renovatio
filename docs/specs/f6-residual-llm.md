# F6 Residual Impedance Reduction via LLM

- **Work item:** `decision-engine-f6/f6-residual-llm`
- **GitHub issue:** #151 (Epic #152)
- **Method:** Agora `spec-driven` with TDD
- **Status:** governed specification
- **Date:** 2026-09-02
- **F5 compatibility baseline:** `ad53256e`

## 1. Outcome

F6 extends the governed LLM infrastructure (already in `renovatio-llm`) to reduce
fine impedance that deterministic + heuristic chains cannot resolve. All LLM
outputs pass through `PromptOutputValidator` + JSON schema. Every decision is
registered in `renovatio-decisions`. Cache is deterministic (three-hash key).
`DeterministicFallback` is always present. The LLM never writes the final file.

## 2. Scope and non-goals

F6 delivers:

1. **Naming enrichment**: LLM suggests idiomátic names for COBOL identifiers
   (`VAR-CLI-NUM-POL` → `policyNumber`) through the existing `DOMAIN_NAMING`
   route (`cobol.domain.naming.v1`). Registered as `NAMING` decisions.
2. **Control flow plan enrichment**: LLM proposes restructuring plans (loop,
   early-return, state machine); deterministic pass applies; characterization
   tests validate via `ControlFlowPlanGate`.
3. **Extended residual routing**: `ResidualRouter` handles new families
   (REDEFINES, COMPUTE overflow, MOVE CORRESPONDING).
4. **Semantic diff**: UI shows paragraph→use-case/method mapping.

F6 does not deliver:

- **Documentation enrichment** (LLM-generated javadoc/TSDoc): deferred to a
  follow-up — it needs its own non-residual generation path and emission seam,
  out of scope here.
- LLM generating final code (plan/naming only)
- Fine-tuning or custom models
- Multi-step agents (single validated calls)
- Full COBOL naming coverage (missing → manual action items)

## 3. Infrastructure dependencies

F6 builds on existing `renovatio-llm` infrastructure:

| Component | Location | F6 extension |
|---|---|---|
| `ResidualRouter` | `llm/residual/` | New construction types |
| `PromptOutputValidator` | `llm/prompt/` | New validators if needed |
| `ControlFlowPlanGate` | `llm/residual/` | Already handles GO TO plans |
| `DeterministicFallback` | `llm/cache/` | Fallback YAML for new prompts |
| `ContentAddressedCache` | `llm/cache/` | Three-hash key (unchanged) |
| `DecisionTransitions` | `decisions/` | `.suggest()` for NAMING category |
| `PromptCatalog` | `resources/prompts/` | New prompt definitions |

## 4. Naming enrichment

### 4.1 Prompt definition

Prompt: `cobol.domain.naming.v1` (the wired `DOMAIN_NAMING` route).

- **Selector:** `DOMAIN_NAMING` (existing route in `ResidualRouter`)
- **Input:** COBOL identifier, context (program, paragraph, level), data type
- **Output schema:** `domain-naming.v1.schema.json`
  (`suggestedName`, optional `boundedContext`, `rationale`)
- **Validators:** `json-schema.v1`, `annotated-ir-reference.v1`, `sanitized-persistence.v1`
- **Fallback:** `cobol.domain.naming.fallback.v1.yaml` → `MANUAL_ACTION`

### 4.2 Decision registration

When the LLM suggests a name it becomes a `DomainNamingPayload` annotation with
review state `NEEDS_REVIEW` (the `domain-naming.v1` schema carries no model
confidence, and `AnnotatedIrSemanticOutputValidator` records confidence as
`0.0`). A confidence-driven `AUTO` vs `SUGGESTED` split is **not** part of F6;
every LLM name suggestion goes through human review before it is applied.

### 4.3 Application

Once accepted by a reviewer, the suggested name is applied to the semantic IR's
`SemanticIdentity` via a deterministic pass. The original COBOL name is
preserved for traceability.

## 5. Documentation enrichment — deferred

Documentation enrichment (LLM-generated Javadoc/TSDoc explaining the original
COBOL and the translation rationale) is **not delivered in F6**. It does not fit
the residual-routing model — every translated unit would get documentation, not
just the residual ~20% — so it needs its own non-residual generation path,
output schema, sanitizer allowlist entries, and an emission seam on
`EmittedArtifact`. Tracked for a follow-up work item.

## 6. Control flow plan enrichment

Already supported by existing `ControlFlowPlanGate` + `cobol.goto.restructure.v1`.
F6 adds:

1. **Extended constructions:** REDEFINES-based state machines, OCCURS DEPENDING ON loops
2. **Gate validation:** plans that break characterization tests are rejected
3. **Deterministic application:** plan is applied by a pass, not by LLM

## 7. Extended residual routing

### 7.1 New construction types

Added to `ResidualConstruction`:
- `COMPUTE_OVERFLOW` — COMPUTE with implicit overflow (always deterministic)
- `MOVE_CORRESPONDING` — MOVE CORRESPONDING with field mismatches

REDEFINES-as-state-machine keeps the existing `REDEFINES` construction and the
`REDEFINES_INTENT` route; F6 adds no separate enum value for it.

### 7.2 New routes

| Construction | Route | Prompt |
|---|---|---|
| `REDEFINES` | `REDEFINES_INTENT` | `cobol.redefines.intent.v1` (existing) |
| `COMPUTE_OVERFLOW` | `DETERMINISTIC` | No LLM (use overflow policy) |
| `MOVE_CORRESPONDING` | `MOVE_CORRESPONDING_INTENT` | `cobol.move-corresponding.intent.v1` (new) |

### 7.3 New prompt: `cobol.move-corresponding.intent.v1`

- **Selector:** `DATA_INTENT.MOVE_CORRESPONDING`
- **Input:** MOVE CORRESPONDING source/target and the paired fields
- **Output schema:** `data-intent.v1.schema.json` (reuse; `construction` enum and
  `cobol-annotated-ir.v1` extended with `MOVE_CORRESPONDING`)
- **Output shape:** `{construction: MOVE_CORRESPONDING, interpretation, assumptions[]}`
- **Validators:** `json-schema.v1`, `annotated-ir-reference.v1`, `sanitized-persistence.v1`
- **Assembler:** `MOVE_CORRESPONDING_INTENT` → `DataIntentPayload` (`AnnotationFamily.DATA_INTENT`),
  always `NEEDS_REVIEW`

## 8. Cache behavior

- Three-hash key: `semanticIrHash + promptVersion + profileHash`
- Second run with no changes → 0 LLM calls (verified by counter)
- `DeterministicFallback` always present for every prompt
- Temperature 0, prompt versioning (unchanged from existing)

## 9. Deterministic fallback

Every prompt has a fallback YAML:
- `cobol.domain.naming.fallback.v1.yaml` → manual action: "Review COBOL identifier naming"
- `move-corresponding.fallback.v1.yaml` → manual action: "Review MOVE CORRESPONDING mapping"

When `profile.llm.enabled=false`, all routes return `DETERMINISTIC`, zero LLM calls.

## 10. Verification

1. **Naming:** fixture with `VAR-CLI-NUM-POL` → `policyNumber`; validated; decision registered
2. **MOVE CORRESPONDING:** fixture with a MOVE CORRESPONDING between mismatched
   groups → `MOVE_CORRESPONDING_INTENT` route → data-intent annotation validated
   against `data-intent.v1` + `cobol-annotated-ir.v1`, pending human review
3. **Control flow:** GO TO spaghetti → LLM plan → deterministic pass → characterization tests green
4. **Cache:** second run with no changes → 0 LLM calls (counter verification)
5. **LLM disabled:** `profile.llm.enabled=false` → complete migration via `DeterministicFallback`
6. **Semantic diff:** UI shows paragraph→use-case mapping for a fixture

## 11. Acceptance mapping

| Agora criterion | Normative sections |
|---|---|
| `naming-enrichment` | §4 defines naming prompt, decision registration, application |
| `documentation-enrichment` | §5 — deferred to a follow-up work item (rationale in §5) |
| `control-flow-enrichment` | §6 extends existing control flow infrastructure |
| `extended-routing` | §7 defines new construction types and routes |
| `cache-behavior` | §8 defines deterministic cache (unchanged) |
| `deterministic-fallback` | §9 defines fallback YAMLs for new prompts |
| `verification` | §10 defines required evidence gates |
