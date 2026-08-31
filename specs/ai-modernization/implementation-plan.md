# Implementation Plan: Governed three-pass COBOL modernization

**Swarm:** `ai-modernization`  
**Work:** `three-pass-modernization`  
**Method:** `spec-driven`

## Plan phases

### 1) Clarify and lock criteria

- Use the completed ADR and child-work evidence as the final boundary for the parent scope.
- Confirm that each criterion in `WORK.md` is explicitly satisfied at `specified` stage.
- Register required artifacts (`spec`, `implementation-plan`) in Agora.

### 2) Deterministic foundation pass

- Enforce strict boundaries so OpenRewrite and IR runtime changes remain pure and deterministic.
- Ensure compile-level consistency and characterization tests are the default verification path.
- Preserve generated identifiers and avoid semantic regressions from repeated runs.

### 3) Enrichment and residual control

- Use governed catalog/runtime flow for LLM prompts and responses.
- Emit and verify sidecar payloads with version/provenance hash fields.
- Keep all residual paths attributed as Agora tool invocations.

### 4) Guardrail hardening

- Add/retain schema validation, deterministic fallback, and manual-action-item generation on failure.
- Ensure file-path fidelity for action items and report regeneration behavior stays stable.
- Keep test plans aligned with characterization fixtures for regression visibility.

### 5) Review-only polish integration

- Keep polish proposals as review artifacts (diff-only).
- Block automatic application of optional polish edits.
- Require human acceptance before any non-deterministic stylistic changes.

## Verification strategy

- Build/test all affected modules after each phase.
- Re-run characterization and schema validation checks as gates before moving between method stages.
- Capture evidence references in Agora evidence records.

## Dependency order

1. Deterministic and characterization work remains the prerequisite for enrichment and polish.
2. Enrichment/residual control must be in place before polished proposals are emitted.
3. Polish remains review-only regardless of deterministic success.
