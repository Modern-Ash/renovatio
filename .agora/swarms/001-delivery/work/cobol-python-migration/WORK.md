---
schema: "agora/work/v1"
id: "cobol-python-migration"
swarm: "delivery"
title: "COBOL to Python migration"
state: "drafting"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"ac-001":"Simple COBOL programs produce Python modules with equivalent observable outputs for representative fixtures.","ac-002":"Local copybooks and COBOL numeric formats, including COMP-3, are resolved or mapped with documented limitations.","ac-003":"Generated Python artifacts include executable tests and a readable transformation report.","ac-004":"The migration is reproducibly invocable through an MCP-compliant tool or CLI with versioned, traceable artifacts."}
satisfied-criteria: []
criterion-statuses: {"ac-001":[],"ac-002":[],"ac-003":[],"ac-004":[]}
required-artifacts: ["spec","implementation-plan"]
child-work-refs: []
budget-limits: null
---

# COBOL to Python migration

## Description

Govern the existing Renovatio COBOL-to-Python migration from draft specification through verified implementation.

## Acceptance criteria

- [ ] **ac-001:** Simple COBOL programs produce Python modules with equivalent observable outputs for representative fixtures.; stages: none
- [ ] **ac-002:** Local copybooks and COBOL numeric formats, including COMP-3, are resolved or mapped with documented limitations.; stages: none
- [ ] **ac-003:** Generated Python artifacts include executable tests and a readable transformation report.; stages: none
- [ ] **ac-004:** The migration is reproducibly invocable through an MCP-compliant tool or CLI with versioned, traceable artifacts.; stages: none

## Required artifacts

- spec
- implementation-plan
