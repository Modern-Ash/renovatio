---
schema: "agora/work/v1"
id: "analysis-workbench-area"
swarm: "renovatio-workbench-analysis"
title: "Theia 5 \u00b7 \u00c1rea de an\u00e1lisis gobernado"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"analysis-adapter":"The Analysis area obtains inventory and run summaries through documented existing-backend adapter endpoints.","read-only-boundary":"Analysis presentation is read-only and does not trigger or alter migration runs.","resilient-states":"Loading, empty, permission-denied and error states remain explicit and accessible.","dashboard-continuity":"The existing dashboard and wizard remain externally decoupled.","verification-evidence":"Contract and local integration evidence are recorded."}
satisfied-criteria: ["analysis-adapter","read-only-boundary","resilient-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"analysis-adapter":["specified","planned","implemented","verified","accepted"],"read-only-boundary":["specified","planned","implemented","verified","accepted"],"resilient-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","analysis-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 5 · Área de análisis gobernado

## Description

Expose existing project inventory, readiness signals and execution runs in Theia through a bounded read-only adapter.

## Acceptance criteria

- [x] **analysis-adapter:** The Analysis area obtains inventory and run summaries through documented existing-backend adapter endpoints.; stages: specified, planned, implemented, verified, accepted
- [x] **read-only-boundary:** Analysis presentation is read-only and does not trigger or alter migration runs.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-states:** Loading, empty, permission-denied and error states remain explicit and accessible.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** The existing dashboard and wizard remain externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract and local integration evidence are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- analysis-contract
- implementation-plan
- verification-report
- review-report
