---
schema: "agora/work/v1"
id: "source-explorer-cobol-jcl"
swarm: "renovatio-workbench-source-explorer"
title: "Theia 9 \u00b7 Source Explorer COBOL/JCL/Copybooks"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"navigable-tree":"A COBOL program opens with a navigable tree of program, sections, paragraphs, copybooks, JCL and datasets.","symbol-outline":"An outline/symbol view lists divisions, paragraphs, PERFORM, CALL, SQL and CICS, each keeping its source origin and position.","search":"Symbol, reference and text search resolve to origin and position within the configured workspace boundary.","file-metadata":"Each file exposes hash, path, encoding and analysis status through a documented backend adapter.","ir-diagnostic-linkage":"Selecting a symbol links to the semantic IR and surfaces parse diagnostics in Problems.","resilient-unsupported":"Unsupported or unparseable files are shown without breaking the workspace; loading, empty, permission-denied and error states stay explicit.","review-boundary":"The explorer is read-only: it cannot modify sources, run analysis or change backend flows; dashboard and wizard stay decoupled.","verification-evidence":"Symbol tests, navigation E2E smoke and contract evidence are recorded."}
satisfied-criteria: ["navigable-tree","symbol-outline","search","file-metadata","ir-diagnostic-linkage","resilient-unsupported","review-boundary","verification-evidence"]
criterion-statuses: {"navigable-tree":["specified","planned","implemented","verified","accepted"],"symbol-outline":["specified","planned","implemented","verified","accepted"],"search":["specified","planned","implemented","verified","accepted"],"file-metadata":["specified","planned","implemented","verified","accepted"],"ir-diagnostic-linkage":["specified","planned","implemented","verified","accepted"],"resilient-unsupported":["specified","planned","implemented","verified","accepted"],"review-boundary":["specified","planned","implemented","verified","accepted"],"verification-evidence":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","source-explorer-contract","implementation-plan","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Theia 9 · Source Explorer COBOL/JCL/Copybooks

## Description

Turn legacy COBOL, copybooks and JCL into a navigable read-only Workbench workspace with symbols, outline, search, file metadata and IR/diagnostic linkage.

## Acceptance criteria

- [x] **navigable-tree:** A COBOL program opens with a navigable tree of program, sections, paragraphs, copybooks, JCL and datasets.; stages: specified, planned, implemented, verified, accepted
- [x] **symbol-outline:** An outline/symbol view lists divisions, paragraphs, PERFORM, CALL, SQL and CICS, each keeping its source origin and position.; stages: specified, planned, implemented, verified, accepted
- [x] **search:** Symbol, reference and text search resolve to origin and position within the configured workspace boundary.; stages: specified, planned, implemented, verified, accepted
- [x] **file-metadata:** Each file exposes hash, path, encoding and analysis status through a documented backend adapter.; stages: specified, planned, implemented, verified, accepted
- [x] **ir-diagnostic-linkage:** Selecting a symbol links to the semantic IR and surfaces parse diagnostics in Problems.; stages: specified, planned, implemented, verified, accepted
- [x] **resilient-unsupported:** Unsupported or unparseable files are shown without breaking the workspace; loading, empty, permission-denied and error states stay explicit.; stages: specified, planned, implemented, verified, accepted
- [x] **review-boundary:** The explorer is read-only: it cannot modify sources, run analysis or change backend flows; dashboard and wizard stay decoupled.; stages: specified, planned, implemented, verified, accepted
- [x] **verification-evidence:** Symbol tests, navigation E2E smoke and contract evidence are recorded.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- source-explorer-contract
- implementation-plan
- verification-report
- review-report
