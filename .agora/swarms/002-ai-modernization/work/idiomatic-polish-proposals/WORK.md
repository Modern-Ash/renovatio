---
schema: "agora/work/v1"
id: "idiomatic-polish-proposals"
swarm: "ai-modernization"
title: "Optional review-only idiomatic polish"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"diff-only":"The polish service emits a reviewable patch artifact and has no automatic apply path.","eligible-only":"A proposal is generated only when schema, compilation, and characterization gates are already green.","human-gate":"Applying any proposed refactor requires explicit human review outside the LLM execution path.","discard-on-failure":"A failed validation discards the patch and records a manual action item without changing generated code."}
satisfied-criteria: []
criterion-statuses: {"diff-only":[],"eligible-only":[],"human-gate":[],"discard-on-failure":[]}
required-artifacts: ["spec","review-policy","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Optional review-only idiomatic polish

## Description

Queue 7 and optional final pass. Depends on annotated-openrewrite-pass and all guardrails. Generate suggested diffs for ports, strategies, naming, and flag collapse only after transliteration is green; never apply them automatically.

## Acceptance criteria

- [ ] **diff-only:** The polish service emits a reviewable patch artifact and has no automatic apply path.; stages: none
- [ ] **eligible-only:** A proposal is generated only when schema, compilation, and characterization gates are already green.; stages: none
- [ ] **human-gate:** Applying any proposed refactor requires explicit human review outside the LLM execution path.; stages: none
- [ ] **discard-on-failure:** A failed validation discards the patch and records a manual action item without changing generated code.; stages: none

## Required artifacts

- spec
- review-policy
- test-report
