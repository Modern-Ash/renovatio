---
schema: "agora/work/v1"
id: "f8-review-fixes"
swarm: "decision-engine-f8-review-fixes"
title: "F8 post-merge review corrections"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"local-confirmation-precedence":"Applying or reapplying a policy never replaces an active locally CONFIRMED or OVERRIDDEN project decision.","legacy-hash-compatibility":"Projects with no reusable bindings retain the exact F1 effective-profile hash and cache identity.","cli-profile-runtime":"A CLI profile apply changes the effective profile consumed by subsequent analyze, plan, and apply commands while retaining explicit binding metadata.","cli-policy-export-runtime":"Normal CLI analysis/review state persists decisions so policy export produces the confirmed catalog without test-only seeding.","stale-policy-signaling":"A matching policy whose selected option was removed or renamed is surfaced as stale and reviewable, never auto-applied or silently reported as an ordinary unmatched decision.","regression-quality":"Focused domain, CLI, API, full reactor, characterization, UI build, and whitespace checks pass."}
satisfied-criteria: ["local-confirmation-precedence","legacy-hash-compatibility","cli-profile-runtime","cli-policy-export-runtime","stale-policy-signaling","regression-quality"]
criterion-statuses: {"local-confirmation-precedence":["specified","planned","implemented","verified","accepted"],"legacy-hash-compatibility":["specified","planned","implemented","verified","accepted"],"cli-profile-runtime":["specified","planned","implemented","verified","accepted"],"cli-policy-export-runtime":["specified","planned","implemented","verified","accepted"],"stale-policy-signaling":["specified","planned","implemented","verified","accepted"],"regression-quality":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# F8 post-merge review corrections

## Description

Follow-up cycle for PR #167 findings: protect local confirmations, preserve unbound legacy hashes, make CLI profile bindings effective, export policies from normal CLI decisions, and report removed policy options as stale without auto-applying them.

## Acceptance criteria

- [x] **local-confirmation-precedence:** Applying or reapplying a policy never replaces an active locally CONFIRMED or OVERRIDDEN project decision.; stages: specified, planned, implemented, verified, accepted
- [x] **legacy-hash-compatibility:** Projects with no reusable bindings retain the exact F1 effective-profile hash and cache identity.; stages: specified, planned, implemented, verified, accepted
- [x] **cli-profile-runtime:** A CLI profile apply changes the effective profile consumed by subsequent analyze, plan, and apply commands while retaining explicit binding metadata.; stages: specified, planned, implemented, verified, accepted
- [x] **cli-policy-export-runtime:** Normal CLI analysis/review state persists decisions so policy export produces the confirmed catalog without test-only seeding.; stages: specified, planned, implemented, verified, accepted
- [x] **stale-policy-signaling:** A matching policy whose selected option was removed or renamed is surfaced as stale and reviewable, never auto-applied or silently reported as an ordinary unmatched decision.; stages: specified, planned, implemented, verified, accepted
- [x] **regression-quality:** Focused domain, CLI, API, full reactor, characterization, UI build, and whitespace checks pass.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
