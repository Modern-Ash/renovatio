---
schema: "agora/work/v1"
id: "live-project-adapter"
swarm: "renovatio-workbench-project-adapter"
title: "Theia 2 \u00b7 Adaptador de proyectos reales autorizado"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"adapter-boundary":"The Workbench obtains project metadata and assets through a documented adapter contract rather than direct wizard imports.","authorization-boundary":"Project access enforces the existing identity and tenant authorization contract without exposing arbitrary workspaces.","resilient-states":"Loading, empty, permission-denied and error states are explicit and accessible.","dashboard-continuity":"The existing dashboard and wizard behavior remain unchanged and externally decoupled.","verification-evidence":"Contract, integration and authorization evidence are recorded before acceptance."}
satisfied-criteria: ["adapter-boundary","authorization-boundary","resilient-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"adapter-boundary":["specified","planned","implemented","verified","accepted"],"authorization-boundary":["specified","planned","implemented","verified","accepted"],"resilient-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","adapter-contract","authorization-model","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 2 · Adaptador de proyectos reales autorizado

## Description

Specify the adapter boundary that replaces the deterministic project fixture with authorized live Renovatio project metadata and assets, preserving the existing dashboard and wizard boundaries.

## Acceptance criteria

- [x] **adapter-boundary:** The Workbench obtains project metadata and assets through a documented adapter contract rather than direct wizard imports.; stages: specified, planned, implemented, verified, accepted
- [x] **authorization-boundary:** Project access enforces the existing identity and tenant authorization contract without exposing arbitrary workspaces.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-states:** Loading, empty, permission-denied and error states are explicit and accessible.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** The existing dashboard and wizard behavior remain unchanged and externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract, integration and authorization evidence are recorded before acceptance.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- adapter-contract
- authorization-model
- implementation-plan
- verification-report
- review-report
