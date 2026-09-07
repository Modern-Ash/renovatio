---
schema: "agora/work/v1"
id: "architecture-workbench-area"
swarm: "renovatio-workbench-architecture"
title: "Theia 6 \u00b7 \u00c1rea de arquitectura gobernada"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"architecture-adapter":"The Architecture area obtains existing domain and target architecture summaries through a documented adapter.","read-only-boundary":"The area cannot apply transformations or alter architecture decisions.","resilient-states":"Loading, empty, permission-denied and error states remain explicit and accessible.","dashboard-continuity":"The existing dashboard and wizard remain externally decoupled.","verification-evidence":"Contract and integration evidence are recorded."}
satisfied-criteria: ["architecture-adapter","read-only-boundary","resilient-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"architecture-adapter":["specified","planned","implemented","verified","accepted"],"read-only-boundary":["specified","planned","implemented","verified","accepted"],"resilient-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","architecture-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 6 · Área de arquitectura gobernada

## Description

Expose existing domain and target architecture views through a project-scoped read-only adapter.

## Acceptance criteria

- [x] **architecture-adapter:** The Architecture area obtains existing domain and target architecture summaries through a documented adapter.; stages: specified, planned, implemented, verified, accepted
- [x] **read-only-boundary:** The area cannot apply transformations or alter architecture decisions.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-states:** Loading, empty, permission-denied and error states remain explicit and accessible.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** The existing dashboard and wizard remain externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract and integration evidence are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- architecture-contract
- implementation-plan
- verification-report
- review-report
