---
schema: "agora/work/v1"
id: "three-pass-modernization"
swarm: "ai-modernization"
title: "Governed three-pass COBOL modernization"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"deterministic-boundary":"Core semantic translation remains pure, repeatable, offline, cacheable, and free of LLM calls.","annotated-ir":"LLM enrichment produces a strict versioned annotated IR sidecar with provenance and content-addressed cache identity.","governed-residual":"Only explicitly classified residual tasks use LLM output and every uncached call is attributable as an Agora tool-run.","guardrails":"Every LLM result passes schema validation, compilation, characterization tests, and human review or falls back deterministically with a manual action item.","review-only-polish":"Post-transliteration idiomatic refactors are proposed as diffs and are never auto-applied."}
satisfied-criteria: ["deterministic-boundary","annotated-ir","governed-residual","guardrails","review-only-polish"]
criterion-statuses: {"deterministic-boundary":["specified","planned","implemented","verified","accepted"],"annotated-ir":["specified","planned","implemented","verified","accepted"],"governed-residual":["specified","planned","implemented","verified","accepted"],"guardrails":["specified","planned","implemented","verified","accepted"],"review-only-polish":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","architecture-decision-record"]
child-work-refs: ["ai-modernization/characterization-guardrails","ai-modernization/deterministic-semantic-core","ai-modernization/annotated-ir-contract","ai-modernization/llm-runtime-catalog-cache","ai-modernization/residual-semantic-enrichment","ai-modernization/annotated-openrewrite-pass","ai-modernization/idiomatic-polish-proposals"]
budget-limits: null
---

# Governed three-pass COBOL modernization

## Description

Organize a reproducible modernization pipeline where deterministic parsing and recipes own core COBOL semantics, governed LLM enrichment operates on versioned IR sidecars, and optional idiomatic polish is review-only. Network calls are forbidden inside OpenRewrite recipes.

## Acceptance criteria

- [x] **deterministic-boundary:** Core semantic translation remains pure, repeatable, offline, cacheable, and free of LLM calls.; stages: specified, planned, implemented, verified, accepted
- [x] **annotated-ir:** LLM enrichment produces a strict versioned annotated IR sidecar with provenance and content-addressed cache identity.; stages: specified, planned, implemented, verified, accepted
- [x] **governed-residual:** Only explicitly classified residual tasks use LLM output and every uncached call is attributable as an Agora tool-run.; stages: specified, planned, implemented, verified, accepted
- [x] **guardrails:** Every LLM result passes schema validation, compilation, characterization tests, and human review or falls back deterministically with a manual action item.; stages: specified, planned, implemented, verified, accepted
- [x] **review-only-polish:** Post-transliteration idiomatic refactors are proposed as diffs and are never auto-applied.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- architecture-decision-record
