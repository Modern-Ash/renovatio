---
schema: "agora/work/v1"
id: "change-sets-approval-rollback"
swarm: "renovatio-workbench-change-sets"
title: "Theia 7 \u00b7 Change sets, aprobaci\u00f3n y rollback"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"state-machine":"Change sets expose draft, review, approved, applied, rejected, and rolled-back states with enforced legal transitions.","mandatory-diff":"Approval requires a viewed and explicitly confirmed diff.","permissions":"Viewer mutations are forbidden and modifying operations require a modify-capable role.","double-confirmation":"Dangerous approval, apply, and rollback require exact confirmation phrases.","deterministic-apply":"Applied artifacts are written through the existing workbench adapter and must match the approved manifest hash.","rollback-audit":"Rollback restores prior generated target content and records who, when, why, and manifest hash.","ui-contract":"Theia exposes an eighth Change Sets area with manifest, diff, approval, apply, rollback, and audit visibility."}
satisfied-criteria: ["state-machine","mandatory-diff","permissions","double-confirmation","deterministic-apply","rollback-audit","ui-contract"]
criterion-statuses: {"state-machine":["specified","planned","implemented","verified","accepted"],"mandatory-diff":["specified","planned","implemented","verified","accepted"],"permissions":["specified","planned","implemented","verified","accepted"],"double-confirmation":["specified","planned","implemented","verified","accepted"],"deterministic-apply":["specified","planned","implemented","verified","accepted"],"rollback-audit":["specified","planned","implemented","verified","accepted"],"ui-contract":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 7 · Change sets, aprobación y rollback

## Description

Convert AI and generation proposals into governed, reviewable, reversible change sets with mandatory diffs, manifest hashes, role permissions, deterministic apply, rollback, and audit history.

## Acceptance criteria

- [x] **state-machine:** Change sets expose draft, review, approved, applied, rejected, and rolled-back states with enforced legal transitions.; stages: specified, planned, implemented, verified, accepted
- [x] **mandatory-diff:** Approval requires a viewed and explicitly confirmed diff.; stages: specified, planned, implemented, verified, accepted
- [x] **permissions:** Viewer mutations are forbidden and modifying operations require a modify-capable role.; stages: specified, planned, implemented, verified, accepted
- [x] **double-confirmation:** Dangerous approval, apply, and rollback require exact confirmation phrases.; stages: specified, planned, implemented, verified, accepted
- [x] **deterministic-apply:** Applied artifacts are written through the existing workbench adapter and must match the approved manifest hash.; stages: specified, planned, implemented, verified, accepted
- [x] **rollback-audit:** Rollback restores prior generated target content and records who, when, why, and manifest hash.; stages: specified, planned, implemented, verified, accepted
- [x] **ui-contract:** Theia exposes an eighth Change Sets area with manifest, diff, approval, apply, rollback, and audit visibility.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
- review-report
