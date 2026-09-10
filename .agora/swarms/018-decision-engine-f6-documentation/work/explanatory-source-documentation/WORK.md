---
schema: "agora/work/v1"
id: "explanatory-source-documentation"
swarm: "decision-engine-f6-documentation"
title: "Emit deterministic explanatory JavaDoc and TSDoc"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"profile-contract":"A validated boolean profile extension enables documentation explicitly and defaults to disabled.","traceable-content":"Documentation identifies the COBOL program and source, effective decision choices, and applied DecisionPoint references in deterministic order.","java-emission":"Enabled Java generation documents each emitted Java unit at a syntactically safe declaration boundary while disabled output remains byte-identical.","node-emission":"Enabled Node generation documents program-specific TypeScript units while project-shared files remain byte-identical across programs and disabled output remains compatible.","safety-determinism":"Untrusted source and decision text cannot terminate or inject documentation, and repeated inputs produce identical bytes.","regression-quality":"Focused tests, full Maven regression tests, build, and patch hygiene pass."}
satisfied-criteria: []
criterion-statuses: {"profile-contract":["specified","planned","implemented","verified"],"traceable-content":["specified","planned","implemented","verified"],"java-emission":["specified","planned","implemented","verified"],"node-emission":["specified","planned","implemented","verified"],"safety-determinism":["specified","planned","implemented","verified"],"regression-quality":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Emit deterministic explanatory JavaDoc and TSDoc

## Description

Add an opt-in profile contract and target-specific emission decorators that explain COBOL provenance and effective translation decisions with DecisionPoint references, without changing default Java/Node bytes or allowing LLM text to write source directly.

## Acceptance criteria

- [ ] **profile-contract:** A validated boolean profile extension enables documentation explicitly and defaults to disabled.; stages: specified, planned, implemented, verified
- [ ] **traceable-content:** Documentation identifies the COBOL program and source, effective decision choices, and applied DecisionPoint references in deterministic order.; stages: specified, planned, implemented, verified
- [ ] **java-emission:** Enabled Java generation documents each emitted Java unit at a syntactically safe declaration boundary while disabled output remains byte-identical.; stages: specified, planned, implemented, verified
- [ ] **node-emission:** Enabled Node generation documents program-specific TypeScript units while project-shared files remain byte-identical across programs and disabled output remains compatible.; stages: specified, planned, implemented, verified
- [ ] **safety-determinism:** Untrusted source and decision text cannot terminate or inject documentation, and repeated inputs produce identical bytes.; stages: specified, planned, implemented, verified
- [ ] **regression-quality:** Focused tests, full Maven regression tests, build, and patch hygiene pass.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
