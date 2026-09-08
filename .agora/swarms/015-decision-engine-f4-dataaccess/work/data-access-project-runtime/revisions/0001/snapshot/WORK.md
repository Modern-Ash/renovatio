---
schema: "agora/work/v1"
id: "data-access-project-runtime"
swarm: "decision-engine-f4-dataaccess"
title: "F4 \u00b7 project-backed data-access classifications"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"real-project-data":"A project with analyzed COBOL programs returns non-empty classified accesses derived from those programs.","project-isolation":"Requests for two projects cannot leak classifications or profile decisions across project boundaries.","effective-profile":"Returned currentStrategy values reflect the effective project profile and source overrides.","safe-empty-contract":"An unanalyzed or empty project returns the documented empty result without synthetic data or server error.","regression-quality":"Focused API/service tests and the relevant Maven regression suite pass."}
satisfied-criteria: ["real-project-data","project-isolation","effective-profile","safe-empty-contract","regression-quality"]
criterion-statuses: {"real-project-data":["specified","planned","implemented","verified","accepted"],"project-isolation":["specified","planned","implemented","verified","accepted"],"effective-profile":["specified","planned","implemented","verified","accepted"],"safe-empty-contract":["specified","planned","implemented","verified","accepted"],"regression-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# F4 · project-backed data-access classifications

## Description

Persist or resolve the project analysis result needed by GET /api/projects/{id}/data-accesses, classify real semantic programs with the effective profile, isolate project state, and preserve deterministic empty/not-found behavior.

## Acceptance criteria

- [x] **real-project-data:** A project with analyzed COBOL programs returns non-empty classified accesses derived from those programs.; stages: specified, planned, implemented, verified, accepted
- [x] **project-isolation:** Requests for two projects cannot leak classifications or profile decisions across project boundaries.; stages: specified, planned, implemented, verified, accepted
- [x] **effective-profile:** Returned currentStrategy values reflect the effective project profile and source overrides.; stages: specified, planned, implemented, verified, accepted
- [x] **safe-empty-contract:** An unanalyzed or empty project returns the documented empty result without synthetic data or server error.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-quality:** Focused API/service tests and the relevant Maven regression suite pass.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
