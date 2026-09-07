---
schema: "agora/work/v1"
id: "deterministic-equivalence-harness"
swarm: "equivalence-rework"
title: "Harness determinista de equivalencia COBOL\u2192target"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "Local equivalence increment implemented and verified; production shadow/canary/cutover remains explicitly deferred until an approved operational environment exists."
status-by: "project:agent"
status-at: "2026-09-06T23:55:00Z"
acceptance-criteria: {"fixture-evidence":"Fixtures record source and target hashes plus business invariants.","classification":"Differences classify as EQUIVALENT, INTENTIONAL_CHANGE, REGRESSION or UNDETERMINED.","reproducibility":"The comparison report is deterministic and testable.","release-boundary":"Regression and undetermined results are visible as release-blocking evidence."}
satisfied-criteria: ["fixture-evidence","classification","reproducibility","release-boundary"]
criterion-statuses: {"fixture-evidence":["specified","planned","implemented","verified","accepted"],"classification":["specified","planned","implemented","verified","accepted"],"reproducibility":["specified","planned","implemented","verified","accepted"],"release-boundary":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# Harness determinista de equivalencia COBOL→target

## Description

Produce reproducible fixture evidence and deterministic difference classification before any operational cutover.

## Acceptance criteria

- [x] **fixture-evidence:** Fixtures record source and target hashes plus business invariants.; stages: specified, planned, implemented, verified, accepted
- [x] **classification:** Differences classify as EQUIVALENT, INTENTIONAL_CHANGE, REGRESSION or UNDETERMINED.; stages: specified, planned, implemented, verified, accepted
- [x] **reproducibility:** The comparison report is deterministic and testable.; stages: specified, planned, implemented, verified, accepted
- [x] **release-boundary:** Regression and undetermined results are visible as release-blocking evidence.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- verification-report
