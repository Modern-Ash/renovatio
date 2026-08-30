---
schema: "agora/work/v1"
id: "characterization-guardrails"
swarm: "ai-modernization"
title: "Characterization harness and non-negotiable gates"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"golden-fixtures":"Representative COBOL programs and advanced residual constructs have committed input-output characterization fixtures.","gate-order":"LLM-assisted outputs are admitted only after schema validation, compilation, characterization tests, and review eligibility checks in that order.","safe-fallback":"Any failed gate discards the proposal and emits deterministic transliteration plus a traceable manual action item.","offline-ci":"Deterministic lanes and cache hits run successfully in CI without network access or provider credentials."}
satisfied-criteria: []
criterion-statuses: {"golden-fixtures":["specified","planned"],"gate-order":["specified","planned"],"safe-fallback":["specified","planned"],"offline-ci":["specified","planned"]}
required-artifacts: ["test-plan","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Characterization harness and non-negotiable gates

## Description

Queue 1 and foundation for every later slice. Establish committed golden fixtures, schema validation, compilation, characterization tests, review gates, and deterministic fallback behavior before any LLM-assisted transformation is eligible.

## Acceptance criteria

- [ ] **golden-fixtures:** Representative COBOL programs and advanced residual constructs have committed input-output characterization fixtures.; stages: specified, planned
- [ ] **gate-order:** LLM-assisted outputs are admitted only after schema validation, compilation, characterization tests, and review eligibility checks in that order.; stages: specified, planned
- [ ] **safe-fallback:** Any failed gate discards the proposal and emits deterministic transliteration plus a traceable manual action item.; stages: specified, planned
- [ ] **offline-ci:** Deterministic lanes and cache hits run successfully in CI without network access or provider credentials.; stages: specified, planned

## Required artifacts

- test-plan
- test-report
