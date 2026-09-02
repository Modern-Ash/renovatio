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
   (`VAR-CLI-NUM-POL` → `policyNumber`). Registered as `NAMING` decisions.
2. **Documentation enrichment**: LLM generates javadoc/docstring explaining what
   the original COBOL did and why the translation decision was taken.
3. **Control flow plan enrichment**: LLM proposes restructuring plans (loop,
   early-return, state machine); deterministic pass applies; characterization
   tests validate via `ControlFlowPlanGate`.
4. **Extended residual routing**: `ResidualRouter` handles new families
   (REDEFINES, COMPUTE overflow, MOVE CORRESPONDING).
5. **Semantic diff**: UI shows paragraph→use-case/method mapping.

F6 does not deliver:

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

New prompt: `cobol.naming.suggest.v1`

- **Selector:** `DOMAIN_NAMING` (existing route in `ResidualRouter`)
- **Input:** COBOL identifier, context (program, paragraph, level), data type
- **Output schema:** `naming-suggestion.v1.schema.json`
  ```json
  {
    "type": "object",
    "properties": {
      "suggestedName": { "type": "string", "minLength": 1, "maxLength": 128 },
      "rationale": { "type": "string", "minLength": 1, "maxLength": 2000 },
      "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
    },
    "required": ["suggestedName", "rationale", "confidence"]
  }
  ```
- **Validators:** `json-schema.v1`, `annotated-ir-reference.v1`
- **Fallback:** `naming-suggestion.fallback.v1.yaml` → `MANUAL_ACTION`

### 4.2 Decision registration

When LLM suggests a name:
1. `DecisionTransitions.suggest()` creates a `NAMING` decision point
2. Source = `LLM`, confidence from LLM output
3. If confidence < 0.6 → human in the loop (status `SUGGESTED`, not `AUTO`)
4. If confidence >= 0.6 → status `AUTO`, applied unless overridden

### 4.3 Application

The suggested name is applied to the semantic IR's `SemanticIdentity` via
a deterministic pass. The original COBOL name is preserved in
`annotations.cooriginalName` for traceability.

## 5. Documentation enrichment

### 5.1 Prompt definition

New prompt: `cobol.documentation.generate.v1`

- **Selector:** `DOCUMENTATION` (new route)
- **Input:** semantic node, COBOL source, translation decisions applied
- **Output schema:** `documentation.v1.schema.json`
  ```json
  {
    "type": "object",
    "properties": {
      "summary": { "type": "string", "minLength": 1, "maxLength": 500 },
      "cobolOriginalBehavior": { "type": "string", "minLength": 1, "maxLength": 1000 },
      "translationRationale": { "type": "string", "minLength": 1, "maxLength": 1000 },
      "parameters": { "type": "array", "items": { "type": "string" } },
      "returnType": { "type": "string" }
    },
    "required": ["summary", "cobolOriginalBehavior", "translationRationale"]
  }
  ```
- **Validators:** `json-schema.v1`
- **Fallback:** `documentation.fallback.v1.yaml` → empty doc, `MANUAL_ACTION`

### 5.2 Emission

The generated documentation is emitted as:
- Java: Javadoc on class/method
- Node: TSDoc on exported function/class
- Attached to the `EmittedArtifact` via a `documentation` field

## 6. Control flow plan enrichment

Already supported by existing `ControlFlowPlanGate` + `cobol.goto.restructure.v1`.
F6 adds:

1. **Extended constructions:** REDEFINES-based state machines, OCCURS DEPENDING ON loops
2. **Gate validation:** plans that break characterization tests are rejected
3. **Deterministic application:** plan is applied by a pass, not by LLM

## 7. Extended residual routing

### 7.1 New construction types

Add to `ResidualConstruction` enum:
- `REDEFINES_STATE_MACHINE` — REDEFINES used as state dispatch
- `COMPUTE_OVERFLOW` — COMPUTE with implicit overflow
- `MOVE_CORRESPONDING` — MOVE CORRESPONDING with field mismatches

### 7.2 New routes

| Construction | Route | Prompt |
|---|---|---|
| `REDEFINES_STATE_MACHINE` | `REDEFINES_INTENT` | `cobol.redefines.intent.v1` (existing) |
| `COMPUTE_OVERFLOW` | `DETERMINISTIC` | No LLM (use overflow policy) |
| `MOVE_CORRESPONDING` | `MOVE_CORRESPONDING_INTENT` | `cobol.move-corresponding.intent.v1` (new) |

### 7.3 New prompt: `cobol.move-corresponding.intent.v1`

- **Input:** MOVE CORRESPONDING source/target, field mappings
- **Output schema:** `data-intent.v1.schema.json` (reuse)
- **Validators:** `json-schema.v1`, `annotated-ir-reference.v1`

## 8. Cache behavior

- Three-hash key: `semanticIrHash + promptVersion + profileHash`
- Second run with no changes → 0 LLM calls (verified by counter)
- `DeterministicFallback` always present for every prompt
- Temperature 0, prompt versioning (unchanged from existing)

## 9. Deterministic fallback

Every new prompt has a fallback YAML:
- `naming-suggestion.fallback.v1.yaml` → manual action: "Review COBOL identifier naming"
- `documentation.fallback.v1.yaml` → manual action: "Add documentation manually"
- `move-corresponding.fallback.v1.yaml` → manual action: "Review MOVE CORRESPONDING mapping"

When `profile.llm.enabled=false`, all routes return `DETERMINISTIC`, zero LLM calls.

## 10. Verification

1. **Naming:** fixture with `VAR-CLI-NUM-POL` → `policyNumber`; validated; decision registered
2. **Documentation:** generated method has javadoc explaining COBOL original + translation rationale
3. **Control flow:** GO TO spaghetti → LLM plan → deterministic pass → characterization tests green
4. **Cache:** second run with no changes → 0 LLM calls (counter verification)
5. **LLM disabled:** `profile.llm.enabled=false` → complete migration via `DeterministicFallback`
6. **Semantic diff:** UI shows paragraph→use-case mapping for a fixture

## 11. Acceptance mapping

| Agora criterion | Normative sections |
|---|---|
| `naming-enrichment` | §4 defines naming prompt, decision registration, application |
| `documentation-enrichment` | §5 defines documentation prompt and emission |
| `control-flow-enrichment` | §6 extends existing control flow infrastructure |
| `extended-routing` | §7 defines new construction types and routes |
| `cache-behavior` | §8 defines deterministic cache (unchanged) |
| `deterministic-fallback` | §9 defines fallback YAMLs for new prompts |
| `verification` | §10 defines required evidence gates |
