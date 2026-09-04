---
schema: "agora/work/v1"
id: "node-multiprogram-generation"
swarm: "decision-engine-node-multiprogram"
title: "Make Node generation composable for multi-program workspaces"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"program-artifacts":"Each COBOL program produces a unique deterministic Node source artifact.","shared-artifacts":"Project-shared Node bootstrap and manifest artifacts are byte-identical across program renders and are written once.","collision-safety":"Identical duplicate artifacts are deduplicated while conflicting duplicate contents still fail before any output is committed.","cli-end-to-end":"The generate command successfully produces one Node project containing artifacts for at least two COBOL programs.","compatibility":"Single-program Node output and Java generation remain compatible with their established contracts.","regression-quality":"Focused tests and the relevant Maven regression suite pass."}
satisfied-criteria: []
criterion-statuses: {"program-artifacts":["specified","planned","implemented","verified"],"shared-artifacts":["specified","planned","implemented","verified"],"collision-safety":["specified","planned","implemented","verified"],"cli-end-to-end":["specified","planned","implemented","verified"],"compatibility":["specified","planned","implemented","verified"],"regression-quality":["specified","planned","implemented","verified"]}
required-artifacts: ["spec","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Make Node generation composable for multi-program workspaces

## Description

Emit deterministic per-program Node artifacts and project-shared files without false duplicate-path failures, while preserving fail-closed behavior for conflicting artifacts and Java compatibility.

## Acceptance criteria

- [ ] **program-artifacts:** Each COBOL program produces a unique deterministic Node source artifact.; stages: specified, planned, implemented, verified
- [ ] **shared-artifacts:** Project-shared Node bootstrap and manifest artifacts are byte-identical across program renders and are written once.; stages: specified, planned, implemented, verified
- [ ] **collision-safety:** Identical duplicate artifacts are deduplicated while conflicting duplicate contents still fail before any output is committed.; stages: specified, planned, implemented, verified
- [ ] **cli-end-to-end:** The generate command successfully produces one Node project containing artifacts for at least two COBOL programs.; stages: specified, planned, implemented, verified
- [ ] **compatibility:** Single-program Node output and Java generation remain compatible with their established contracts.; stages: specified, planned, implemented, verified
- [ ] **regression-quality:** Focused tests and the relevant Maven regression suite pass.; stages: specified, planned, implemented, verified

## Required artifacts

- spec
- implementation-plan
- verification-report
- review-report
