---
schema: "agora/work/v1"
id: "runtime-security-workspace-sandbox"
swarm: "issue-231-security-hardening"
title: "AC-10: Harden API, MCP and Workbench for safe operation"
state: "verifying"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"deployment-modes":"The alpha declares and enforces a loopback local-only mode by default; remote mode requires explicit configuration and full controls.","authentication":"X-Role is no longer treated as trusted production identity; remote mode uses an approved mechanism and local mode is explicit.","authorization":"Read/plan/apply/admin permissions are checked in application services and have project-isolation tests.","workspace-sandbox":"All paths are canonicalized under allowed roots; traversal, symlink escape, unauthorized absolute paths and known TOCTOU cases are blocked.","safe-apply":"Preview is read-only; apply requires explicit intent, current manifest, file/size limits and recoverable rollback.","web-hardening":"CORS, CSRF, headers, error redaction, request limits, health details and upload handling have safe defaults.","auditability":"Sensitive actions emit structured audit events with actor, project, manifest and result without secrets or full source.","security-tests":"Threat model and negative tests cover API, MCP, Workbench, filesystem and LLM boundary."}
satisfied-criteria: []
criterion-statuses: {"deployment-modes":["specified","planned","implemented"],"authentication":["specified","planned"],"authorization":["specified","planned"],"workspace-sandbox":["specified","planned","implemented"],"safe-apply":["specified","planned","implemented"],"web-hardening":["specified","planned"],"auditability":["specified","planned"],"security-tests":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","threat-model","security-report","operations-guide","test-report"]
child-work-refs: []
budget-limits: null
---

# AC-10: Harden API, MCP and Workbench for safe operation

## Description

Issue #231. Define and enforce local-only by default, fail-closed remote mode, trusted identity boundary, application-service authorization, workspace path sandboxing, safe apply, web hardening, auditability and negative security tests for API, MCP and Workbench.

## Acceptance criteria

- [ ] **deployment-modes:** The alpha declares and enforces a loopback local-only mode by default; remote mode requires explicit configuration and full controls.; stages: specified, planned, implemented
- [ ] **authentication:** X-Role is no longer treated as trusted production identity; remote mode uses an approved mechanism and local mode is explicit.; stages: specified, planned
- [ ] **authorization:** Read/plan/apply/admin permissions are checked in application services and have project-isolation tests.; stages: specified, planned
- [ ] **workspace-sandbox:** All paths are canonicalized under allowed roots; traversal, symlink escape, unauthorized absolute paths and known TOCTOU cases are blocked.; stages: specified, planned, implemented
- [ ] **safe-apply:** Preview is read-only; apply requires explicit intent, current manifest, file/size limits and recoverable rollback.; stages: specified, planned, implemented
- [ ] **web-hardening:** CORS, CSRF, headers, error redaction, request limits, health details and upload handling have safe defaults.; stages: specified, planned
- [ ] **auditability:** Sensitive actions emit structured audit events with actor, project, manifest and result without secrets or full source.; stages: specified, planned
- [ ] **security-tests:** Threat model and negative tests cover API, MCP, Workbench, filesystem and LLM boundary.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- threat-model
- security-report
- operations-guide
- test-report
