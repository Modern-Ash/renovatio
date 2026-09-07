---
schema: "agora/work/v1"
id: "durable-workbench-context"
swarm: "renovatio-workbench-persistence"
title: "Theia 4 \u00b7 Persistencia de contexto de Workbench"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"persistent-context":"Project selection and permitted non-sensitive view context survive a Workbench restart through the adapter contract.","scoped-storage":"Persisted state is scoped to the selected project and contains neither credentials nor arbitrary workspace paths.","recovery-states":"Unavailable, stale and malformed persisted state resolves to an accessible safe state.","dashboard-continuity":"The existing dashboard and wizard remain externally decoupled.","verification-evidence":"Contract, migration and recovery checks are recorded."}
satisfied-criteria: ["persistent-context","scoped-storage","recovery-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"persistent-context":["specified","planned","implemented","verified","accepted"],"scoped-storage":["specified","planned","implemented","verified","accepted"],"recovery-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","persistence-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 4 · Persistencia de contexto de Workbench

## Description

Persist project selection and non-sensitive Workbench context through a bounded Renovatio backend contract, retaining a safe local fallback and deferring identity/login.

## Acceptance criteria

- [x] **persistent-context:** Project selection and permitted non-sensitive view context survive a Workbench restart through the adapter contract.; stages: specified, planned, implemented, verified, accepted
- [x] **scoped-storage:** Persisted state is scoped to the selected project and contains neither credentials nor arbitrary workspace paths.; stages: specified, planned, implemented, verified, accepted
- [x] **recovery-states:** Unavailable, stale and malformed persisted state resolves to an accessible safe state.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** The existing dashboard and wizard remain externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract, migration and recovery checks are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- persistence-contract
- implementation-plan
- verification-report
- review-report
