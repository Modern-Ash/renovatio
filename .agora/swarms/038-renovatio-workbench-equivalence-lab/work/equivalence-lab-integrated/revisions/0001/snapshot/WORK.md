---
schema: "agora/work/v1"
id: "equivalence-lab-integrated"
swarm: "renovatio-workbench-equivalence-lab"
title: "Theia 8 \u00b7 Equivalence Lab integrado"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"fixture-selector":" Sources, cases, baseline and candidate are selectable from the workbench contract.","execution-repeatability":" A COBOL case executes without leaving Theia and exact repetition is possible from prior run evidence.","async-control":" Runs expose progress, logs and cancellation semantics through governed endpoints and UI affordances.","divergence-triage":" State, outputs, files and SQL comparisons surface navigable divergence evidence with accept/defect/AI-analysis triage actions.","promotion-gate":" Promotion readiness is blocked while criteria fail or divergences remain unaccepted.","audited-export":" Reports export with commit/profile/change-set/hash history and audit entries.","verification":" Smoke/divergence fixtures, API coverage, UI contract tests and build pass."}
satisfied-criteria: ["fixture-selector","execution-repeatability","async-control","divergence-triage","promotion-gate","audited-export","verification"]
criterion-statuses: {"fixture-selector":["specified","planned","implemented","verified","accepted"],"execution-repeatability":["specified","planned","implemented","verified","accepted"],"async-control":["specified","planned","implemented","verified","accepted"],"divergence-triage":["specified","planned","implemented","verified","accepted"],"promotion-gate":["specified","planned","implemented","verified","accepted"],"audited-export":["specified","planned","implemented","verified","accepted"],"verification":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","implementation","verification","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 8 · Equivalence Lab integrado

## Description

Deliver an integrated Theia Equivalence Lab that executes COBOL/target equivalence cases from the workbench, captures repeatable evidence, supports divergence triage, gates promotion, and exports audited reports.

## Acceptance criteria

- [x] **fixture-selector:**  Sources, cases, baseline and candidate are selectable from the workbench contract.; stages: specified, planned, implemented, verified, accepted
- [x] **execution-repeatability:**  A COBOL case executes without leaving Theia and exact repetition is possible from prior run evidence.; stages: specified, planned, implemented, verified, accepted
- [x] **async-control:**  Runs expose progress, logs and cancellation semantics through governed endpoints and UI affordances.; stages: specified, planned, implemented, verified, accepted
- [x] **divergence-triage:**  State, outputs, files and SQL comparisons surface navigable divergence evidence with accept/defect/AI-analysis triage actions.; stages: specified, planned, implemented, verified, accepted
- [x] **promotion-gate:**  Promotion readiness is blocked while criteria fail or divergences remain unaccepted.; stages: specified, planned, implemented, verified, accepted
- [x] **audited-export:**  Reports export with commit/profile/change-set/hash history and audit entries.; stages: specified, planned, implemented, verified, accepted
- [x] **verification:**  Smoke/divergence fixtures, API coverage, UI contract tests and build pass.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- implementation
- verification
- review-report
