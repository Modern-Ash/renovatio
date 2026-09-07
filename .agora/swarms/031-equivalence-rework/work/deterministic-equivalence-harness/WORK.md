---
schema: "agora/work/v1"
id: "deterministic-equivalence-harness"
swarm: "equivalence-rework"
title: "Harness determinista de equivalencia COBOL\u2192target"
state: "verifying"
revision: 2
operational-status: "revalidation"
status-reason: "Publish verified implementation changes through governed repository commit"
status-by: "project:owner"
status-at: "2026-09-07T00:13:27.945349Z"
acceptance-criteria: {"fixture-evidence":"Fixtures record source and target hashes plus business invariants.","classification":"Differences classify as EQUIVALENT, INTENTIONAL_CHANGE, REGRESSION or UNDETERMINED.","reproducibility":"The comparison report is deterministic and testable.","release-boundary":"Regression and undetermined results are visible as release-blocking evidence."}
satisfied-criteria: []
criterion-statuses: {"fixture-evidence":[],"classification":[],"reproducibility":[],"release-boundary":[]}
required-artifacts: ["spec","implementation-plan","verification-report"]
child-work-refs: []
budget-limits: null
---

# Harness determinista de equivalencia COBOL→target

## Description

Produce reproducible fixture evidence and deterministic difference classification before any operational cutover.

## Acceptance criteria

- [ ] **fixture-evidence:** Fixtures record source and target hashes plus business invariants.; stages: none
- [ ] **classification:** Differences classify as EQUIVALENT, INTENTIONAL_CHANGE, REGRESSION or UNDETERMINED.; stages: none
- [ ] **reproducibility:** The comparison report is deterministic and testable.; stages: none
- [ ] **release-boundary:** Regression and undetermined results are visible as release-blocking evidence.; stages: none

## Required artifacts

- spec
- implementation-plan
- verification-report
