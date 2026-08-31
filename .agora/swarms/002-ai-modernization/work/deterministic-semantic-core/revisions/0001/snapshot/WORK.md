---
schema: "agora/work/v1"
id: "deterministic-semantic-core"
swarm: "ai-modernization"
title: "Deterministic COBOL semantic core"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "Repository write authority is available in the supervising Agora session; the #122 characterization dependency remains a verification gate, not an implementation blocker."
status-by: "project:agent"
status-at: "2026-08-31T01:19:58.121296Z"
acceptance-criteria: {"statements":"MOVE, COMPUTE, IF, EVALUATE, and simple PERFORM have deterministic IR and translation coverage.","data-model":"Basic PIC mapping uses the rich type model and level-88 conditions map to typed enum or equivalent deterministic constructs.","pure-recipes":"OpenRewrite recipes make no network or LLM calls and produce byte-stable output for identical inputs.","characterized":"Unit and characterization tests prove observable behavior for every supported construct."}
satisfied-criteria: ["statements","data-model","pure-recipes","characterized"]
criterion-statuses: {"statements":["specified","planned","implemented","verified","accepted"],"data-model":["specified","planned","implemented","verified","accepted"],"pure-recipes":["specified","planned","implemented","verified","accepted"],"characterized":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
parent-work: "ai-modernization/three-pass-modernization"
---

# Deterministic COBOL semantic core

## Description

Queue 2. Depends on characterization-guardrails and may run in parallel with annotated-ir-contract. Extend parser, IR, runtime, and pure recipes for MOVE, COMPUTE, IF, EVALUATE, simple PERFORM, basic PIC mapping, and level-88 enums.

## Acceptance criteria

- [x] **statements:** MOVE, COMPUTE, IF, EVALUATE, and simple PERFORM have deterministic IR and translation coverage.; stages: specified, planned, implemented, verified, accepted
- [x] **data-model:** Basic PIC mapping uses the rich type model and level-88 conditions map to typed enum or equivalent deterministic constructs.; stages: specified, planned, implemented, verified, accepted
- [x] **pure-recipes:** OpenRewrite recipes make no network or LLM calls and produce byte-stable output for identical inputs.; stages: specified, planned, implemented, verified, accepted
- [x] **characterized:** Unit and characterization tests prove observable behavior for every supported construct.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- test-report
