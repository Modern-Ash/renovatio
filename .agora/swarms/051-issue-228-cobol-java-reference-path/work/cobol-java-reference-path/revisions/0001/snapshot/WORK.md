---
schema: "agora/work/v1"
id: "cobol-java-reference-path"
swarm: "issue-228-cobol-java-reference-path"
title: "AC-07: Close the COBOL-to-Java reference vertical"
state: "completed"
revision: 1
operational-status: "active"
status-reason: "The user requested closure of the Agora cycle, which authorizes remediation of the acceptance blockers documented by the post-merge audit; proceed under the corrective specification."
status-by: "project:agent"
status-at: "2026-09-10T13:01:48.509542Z"
acceptance-criteria: {"fixtures":"Three redistributable batch, CICS/MVC, and DB2 fixtures include source inputs, copybooks, decisions, manifests, and expected Java outputs.","end-to-end":"Each fixture executes the canonical discovery, parse, IR, decision, manifest, Java emission, build, and equivalence path.","determinism":"Two offline executions of the same snapshot produce byte-identical paths and content except explicitly excluded metadata.","idempotency":"Reapplying the same input does not introduce changes and stale inputs are rejected or reported.","semantic-gaps":"Unsupported semantics produce stable visible action items and cannot silently pass as successful TODO output.","equivalence":"Characterization and equivalence account for expected, missing, and unexpected generated files with explicit gate outcomes.","surface-proof":"The same project pipeline is reachable through the application service surface, MCP adapter, CLI command, and REST API without divergent generation behavior.","runbook":"A new contributor can reproduce the offline demonstration in under ten minutes using the committed runbook."}
satisfied-criteria: ["fixtures","end-to-end","determinism","idempotency","semantic-gaps","equivalence","runbook","surface-proof"]
criterion-statuses: {"fixtures":["specified","planned","implemented","verified","accepted"],"end-to-end":["specified","planned","implemented","verified","accepted"],"determinism":["specified","planned","implemented","verified","accepted"],"idempotency":["specified","planned","implemented","verified","accepted"],"semantic-gaps":["specified","planned","implemented","verified","accepted"],"equivalence":["specified","planned","implemented","verified","accepted"],"surface-proof":["specified","planned","implemented","verified","accepted"],"runbook":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","reference-fixtures","demo-runbook","equivalence-report","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-07: Close the COBOL-to-Java reference vertical

## Description

Retrospective governance reconstruction for issue #228. PR #249, review remediation, CI, merge commit 78c4580f, and automatic issue closure existed before this durable work record; this cycle audits and records that history without rewriting it.

## Acceptance criteria

- [x] **fixtures:** Three redistributable batch, CICS/MVC, and DB2 fixtures include source inputs, copybooks, decisions, manifests, and expected Java outputs.; stages: specified, planned, implemented, verified, accepted
- [x] **end-to-end:** Each fixture executes the canonical discovery, parse, IR, decision, manifest, Java emission, build, and equivalence path.; stages: specified, planned, implemented, verified, accepted
- [x] **determinism:** Two offline executions of the same snapshot produce byte-identical paths and content except explicitly excluded metadata.; stages: specified, planned, implemented, verified, accepted
- [x] **idempotency:** Reapplying the same input does not introduce changes and stale inputs are rejected or reported.; stages: specified, planned, implemented, verified, accepted
- [x] **semantic-gaps:** Unsupported semantics produce stable visible action items and cannot silently pass as successful TODO output.; stages: specified, planned, implemented, verified, accepted
- [x] **equivalence:** Characterization and equivalence account for expected, missing, and unexpected generated files with explicit gate outcomes.; stages: specified, planned, implemented, verified, accepted
- [x] **surface-proof:** The same project pipeline is reachable through the application service surface, MCP adapter, CLI command, and REST API without divergent generation behavior.; stages: specified, planned, implemented, verified, accepted
- [x] **runbook:** A new contributor can reproduce the offline demonstration in under ten minutes using the committed runbook.; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- reference-fixtures
- demo-runbook
- equivalence-report
- test-report
