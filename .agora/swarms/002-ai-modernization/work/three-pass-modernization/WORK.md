---
schema: "agora/work/v1"
id: "three-pass-modernization"
swarm: "ai-modernization"
title: "Governed three-pass COBOL modernization"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"deterministic-boundary":"Core semantic translation remains pure, repeatable, offline, cacheable, and free of LLM calls.","annotated-ir":"LLM enrichment produces a strict versioned annotated IR sidecar with provenance and content-addressed cache identity.","governed-residual":"Only explicitly classified residual tasks use LLM output and every uncached call is attributable as an Agora tool-run.","guardrails":"Every LLM result passes schema validation, compilation, characterization tests, and human review or falls back deterministically with a manual action item.","review-only-polish":"Post-transliteration idiomatic refactors are proposed as diffs and are never auto-applied."}
satisfied-criteria: []
criterion-statuses: {"deterministic-boundary":[],"annotated-ir":[],"governed-residual":[],"guardrails":[],"review-only-polish":[]}
required-artifacts: ["spec","implementation-plan","architecture-decision-record"]
child-work-refs: ["ai-modernization/characterization-guardrails","ai-modernization/deterministic-semantic-core","ai-modernization/annotated-ir-contract","ai-modernization/llm-runtime-catalog-cache","ai-modernization/residual-semantic-enrichment","ai-modernization/annotated-openrewrite-pass","ai-modernization/idiomatic-polish-proposals"]
budget-limits: null
---

# Governed three-pass COBOL modernization

## Description

Organize a reproducible modernization pipeline where deterministic parsing and recipes own core COBOL semantics, governed LLM enrichment operates on versioned IR sidecars, and optional idiomatic polish is review-only. Network calls are forbidden inside OpenRewrite recipes.

## Acceptance criteria

- [ ] **deterministic-boundary:** Core semantic translation remains pure, repeatable, offline, cacheable, and free of LLM calls.; stages: none
- [ ] **annotated-ir:** LLM enrichment produces a strict versioned annotated IR sidecar with provenance and content-addressed cache identity.; stages: none
- [ ] **governed-residual:** Only explicitly classified residual tasks use LLM output and every uncached call is attributable as an Agora tool-run.; stages: none
- [ ] **guardrails:** Every LLM result passes schema validation, compilation, characterization tests, and human review or falls back deterministically with a manual action item.; stages: none
- [ ] **review-only-polish:** Post-transliteration idiomatic refactors are proposed as diffs and are never auto-applied.; stages: none

## Required artifacts

- spec
- implementation-plan
- architecture-decision-record
