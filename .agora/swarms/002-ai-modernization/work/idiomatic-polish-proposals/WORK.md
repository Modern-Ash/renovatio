---
schema: "agora/work/v1"
id: "idiomatic-polish-proposals"
swarm: "ai-modernization"
title: "Optional review-only idiomatic polish"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"diff-only":"The polish service emits a reviewable patch artifact and has no automatic apply path.","eligible-only":"A proposal is generated only when schema, compilation, and characterization gates are already green.","human-gate":"Applying any proposed refactor requires explicit human review outside the LLM execution path.","discard-on-failure":"A failed validation discards the patch and records a manual action item without changing generated code."}
satisfied-criteria: []
criterion-statuses: {"diff-only":["specified","planned","implemented","verified"],"eligible-only":["specified","planned","implemented","verified"],"human-gate":["specified","planned","implemented","verified"],"discard-on-failure":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","review-policy","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Optional review-only idiomatic polish

## Description

Queue 7 and optional final pass. Depends on annotated-openrewrite-pass and all guardrails. Generate suggested diffs for ports, strategies, naming, and flag collapse only after transliteration is green; never apply them automatically.

## Acceptance criteria

- [ ] **diff-only:** The polish service emits a reviewable patch artifact and has no automatic apply path.; stages: specified, planned, implemented, verified
- [ ] **eligible-only:** A proposal is generated only when schema, compilation, and characterization gates are already green.; stages: specified, planned, implemented, verified
- [ ] **human-gate:** Applying any proposed refactor requires explicit human review outside the LLM execution path.; stages: specified, planned, implemented, verified
- [ ] **discard-on-failure:** A failed validation discards the patch and records a manual action item without changing generated code.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- review-policy
- test-report
