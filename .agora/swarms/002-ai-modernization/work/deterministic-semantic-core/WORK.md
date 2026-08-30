---
schema: "agora/work/v1"
id: "deterministic-semantic-core"
swarm: "ai-modernization"
title: "Deterministic COBOL semantic core"
state: "clarified"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"statements":"MOVE, COMPUTE, IF, EVALUATE, and simple PERFORM have deterministic IR and translation coverage.","data-model":"Basic PIC mapping uses the rich type model and level-88 conditions map to typed enum or equivalent deterministic constructs.","pure-recipes":"OpenRewrite recipes make no network or LLM calls and produce byte-stable output for identical inputs.","characterized":"Unit and characterization tests prove observable behavior for every supported construct."}
satisfied-criteria: []
criterion-statuses: {"statements":["specified"],"data-model":["specified"],"pure-recipes":["specified"],"characterized":["specified"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Deterministic COBOL semantic core

## Description

Queue 2. Depends on characterization-guardrails and may run in parallel with annotated-ir-contract. Extend parser, IR, runtime, and pure recipes for MOVE, COMPUTE, IF, EVALUATE, simple PERFORM, basic PIC mapping, and level-88 enums.

## Acceptance criteria

- [ ] **statements:** MOVE, COMPUTE, IF, EVALUATE, and simple PERFORM have deterministic IR and translation coverage.; stages: specified
- [ ] **data-model:** Basic PIC mapping uses the rich type model and level-88 conditions map to typed enum or equivalent deterministic constructs.; stages: specified
- [ ] **pure-recipes:** OpenRewrite recipes make no network or LLM calls and produce byte-stable output for identical inputs.; stages: specified
- [ ] **characterized:** Unit and characterization tests prove observable behavior for every supported construct.; stages: specified

## Required artifacts

- spec
- implementation-plan
- test-report
