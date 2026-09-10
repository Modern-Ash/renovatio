---
schema: "agora/work/v1"
id: "cobol-java-reference-path"
swarm: "issue-228-cobol-java-reference-path"
title: "AC-07: Close the COBOL-to-Java reference vertical"
state: "clarified"
revision: 1
operational-status: "active"
status-reason: "The user requested closure of the Agora cycle, which authorizes remediation of the acceptance blockers documented by the post-merge audit; proceed under the corrective specification."
status-by: "project:agent"
status-at: "2026-09-10T13:01:48.509542Z"
acceptance-criteria: {"fixtures":"Three redistributable batch, CICS/MVC, and DB2 fixtures include source inputs, copybooks, decisions, manifests, and expected Java outputs.","end-to-end":"Each fixture executes the canonical discovery, parse, IR, decision, manifest, Java emission, build, and equivalence path.","determinism":"Two offline executions of the same snapshot produce byte-identical paths and content except explicitly excluded metadata.","idempotency":"Reapplying the same input does not introduce changes and stale inputs are rejected or reported.","semantic-gaps":"Unsupported semantics produce stable visible action items and cannot silently pass as successful TODO output.","equivalence":"Characterization and equivalence account for expected, missing, and unexpected generated files with explicit gate outcomes.","surface-proof":"The same project pipeline is reachable through the application service surface and MCP-facing adapter without divergent generation behavior.","runbook":"A new contributor can reproduce the offline demonstration in under ten minutes using the committed runbook."}
satisfied-criteria: []
criterion-statuses: {"fixtures":["specified"],"end-to-end":["specified"],"determinism":["specified"],"idempotency":["specified"],"semantic-gaps":["specified"],"equivalence":["specified"],"surface-proof":["specified"],"runbook":["specified"]}
required-artifacts: ["spec","implementation-plan","reference-fixtures","demo-runbook","equivalence-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-07: Close the COBOL-to-Java reference vertical

## Description

Retrospective governance reconstruction for issue #228. PR #249, review remediation, CI, merge commit 78c4580f, and automatic issue closure existed before this durable work record; this cycle audits and records that history without rewriting it.

## Acceptance criteria

- [ ] **fixtures:** Three redistributable batch, CICS/MVC, and DB2 fixtures include source inputs, copybooks, decisions, manifests, and expected Java outputs.; stages: specified
- [ ] **end-to-end:** Each fixture executes the canonical discovery, parse, IR, decision, manifest, Java emission, build, and equivalence path.; stages: specified
- [ ] **determinism:** Two offline executions of the same snapshot produce byte-identical paths and content except explicitly excluded metadata.; stages: specified
- [ ] **idempotency:** Reapplying the same input does not introduce changes and stale inputs are rejected or reported.; stages: specified
- [ ] **semantic-gaps:** Unsupported semantics produce stable visible action items and cannot silently pass as successful TODO output.; stages: specified
- [ ] **equivalence:** Characterization and equivalence account for expected, missing, and unexpected generated files with explicit gate outcomes.; stages: specified
- [ ] **surface-proof:** The same project pipeline is reachable through the application service surface and MCP-facing adapter without divergent generation behavior.; stages: specified
- [ ] **runbook:** A new contributor can reproduce the offline demonstration in under ten minutes using the committed runbook.; stages: specified

## Required artifacts

- spec
- implementation-plan
- reference-fixtures
- demo-runbook
- equivalence-report
- test-report
