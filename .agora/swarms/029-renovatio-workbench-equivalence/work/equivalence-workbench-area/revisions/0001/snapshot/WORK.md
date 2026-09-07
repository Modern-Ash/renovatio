---
schema: "agora/work/v1"
id: "equivalence-workbench-area"
swarm: "renovatio-workbench-equivalence"
title: "Theia 8 \u00b7 \u00c1rea de equivalencia gobernada"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"equivalence-adapter":"The Equivalence area obtains existing evidence summaries through a documented backend adapter.","review-boundary":"The area exposes review or acceptance status but cannot run comparisons, accept results, or apply changes.","resilient-states":"Loading, empty, permission-denied and error states remain explicit and safe.","dashboard-continuity":"Dashboard and wizard remain externally decoupled.","verification-evidence":"Contract and integration evidence are recorded."}
satisfied-criteria: ["equivalence-adapter","review-boundary","resilient-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"equivalence-adapter":["specified","planned","implemented","verified","accepted"],"review-boundary":["specified","planned","implemented","verified","accepted"],"resilient-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","equivalence-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 8 · Área de equivalencia gobernada

## Description

Expose existing equivalence evidence, test deltas and acceptance history through a read-only Workbench adapter.

## Acceptance criteria

- [x] **equivalence-adapter:** The Equivalence area obtains existing evidence summaries through a documented backend adapter.; stages: specified, planned, implemented, verified, accepted
- [x] **review-boundary:** The area exposes review or acceptance status but cannot run comparisons, accept results, or apply changes.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-states:** Loading, empty, permission-denied and error states remain explicit and safe.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** Dashboard and wizard remain externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract and integration evidence are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- equivalence-contract
- implementation-plan
- verification-report
- review-report
