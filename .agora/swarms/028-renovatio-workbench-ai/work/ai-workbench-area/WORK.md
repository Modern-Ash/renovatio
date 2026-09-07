---
schema: "agora/work/v1"
id: "ai-workbench-area"
swarm: "renovatio-workbench-ai"
title: "Theia 7 \u00b7 \u00c1rea de sugerencias AI gobernadas"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"ai-adapter":"The AI area obtains recommendation and provenance summaries through a documented adapter.","review-boundary":"The area exposes review status but cannot apply recommendations.","resilient-states":"Loading, empty, permission-denied and error states remain explicit.","dashboard-continuity":"Dashboard and wizard remain externally decoupled.","verification-evidence":"Contract and integration evidence are recorded."}
satisfied-criteria: ["ai-adapter","review-boundary","resilient-states","dashboard-continuity","verification-evidence"]
criterion-statuses: {"ai-adapter":["specified","planned","implemented","verified","accepted"],"review-boundary":["specified","planned","implemented","verified","accepted"],"resilient-states":["specified","planned","implemented","verified","accepted"],"dashboard-continuity":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","ai-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 7 · Área de sugerencias AI gobernadas

## Description

Expose existing governed recommendations, provenance and review boundaries through a read-only Workbench adapter.

## Acceptance criteria

- [x] **ai-adapter:** The AI area obtains recommendation and provenance summaries through a documented adapter.; stages: specified, planned, implemented, verified, accepted
- [x] **review-boundary:** The area exposes review status but cannot apply recommendations.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-states:** Loading, empty, permission-denied and error states remain explicit.; stages: specified, planned, implemented, verified, accepted
- [x] **dashboard-continuity:** Dashboard and wizard remain externally decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Contract and integration evidence are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- ai-contract
- implementation-plan
- verification-report
- review-report
