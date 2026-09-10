---
schema: "agora/work/v1"
id: "renovatio-api"
swarm: "api-layer"
title: "renovatio-api: REST API + async jobs + persistence (issue #130 phase 2)"
state: "completed"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"rest-endpoints":"POST/GET /api/projects, POST /api/projects/{id}/jobs, GET /api/jobs/{id}, GET /api/jobs/{id}/events (SSE), GET /api/projects/{id}/plan|runs/{runId}/diff|metrics|action-items, POST /api/action-items/{id}/status","async-jobs":"POST /api/projects/{id}/jobs returns jobId immediately; GET /api/jobs/{id} shows status/progress/result; SSE endpoint streams job progress events; ThreadPoolTaskExecutor processes jobs async","persistence":"H2 embedded database with JPA entities for Project, Job, MigrationPlanSnapshot, RunSnapshot, ActionItem; PersistentPlanStore wraps MigrationPlanService enabling state survives restart","role-gating":"Reuses AccessRole/ReportAccessService; X-Role header controls access to endpoints; ADMIN/MANAGER can view/modify, VIEWER read-only","no-regression":"mvn clean install green; MCP server unchanged; renovatio-cli unchanged; existing ReportController tests pass","tested":"Unit tests for controllers, services, repositories; integration tests for full job lifecycle; SSE event stream test"}
satisfied-criteria: ["rest-endpoints","async-jobs","persistence","role-gating","no-regression","tested"]
criterion-statuses: {"rest-endpoints":["specified","planned","implemented","verified","accepted"],"async-jobs":["specified","planned","implemented","verified","accepted"],"persistence":["specified","planned","implemented","verified","accepted"],"role-gating":["specified","planned","implemented","verified","accepted"],"no-regression":["specified","planned","implemented","verified","accepted"],"tested":["specified","planned","implemented","verified","accepted"]}
required-artifacts: ["spec","implementation-plan","test-report"]
child-work-refs: []
budget-limits: null
---

# renovatio-api: REST API + async jobs + persistence (issue #130 phase 2)

## Description

New Maven module renovatio-api: Spring Boot web module with REST endpoints oriented to UI (not JSON-RPC), async job execution with SSE progress, H2 embedded persistence for Project/Job/PlanSnapshot/RunSnapshot/ActionItem, and PersistentPlanStore wrapping MigrationPlanService. Reuses AccessRole/ReportAccessService for role-based gating.

## Acceptance criteria

- [x] **rest-endpoints:** POST/GET /api/projects, POST /api/projects/{id}/jobs, GET /api/jobs/{id}, GET /api/jobs/{id}/events (SSE), GET /api/projects/{id}/plan|runs/{runId}/diff|metrics|action-items, POST /api/action-items/{id}/status; stages: specified, planned, implemented, verified, accepted
- [x] **async-jobs:** POST /api/projects/{id}/jobs returns jobId immediately; GET /api/jobs/{id} shows status/progress/result; SSE endpoint streams job progress events; ThreadPoolTaskExecutor processes jobs async; stages: specified, planned, implemented, verified, accepted
- [x] **persistence:** H2 embedded database with JPA entities for Project, Job, MigrationPlanSnapshot, RunSnapshot, ActionItem; PersistentPlanStore wraps MigrationPlanService enabling state survives restart; stages: specified, planned, implemented, verified, accepted
- [x] **role-gating:** Reuses AccessRole/ReportAccessService; X-Role header controls access to endpoints; ADMIN/MANAGER can view/modify, VIEWER read-only; stages: specified, planned, implemented, verified, accepted
- [x] **no-regression:** mvn clean install green; MCP server unchanged; renovatio-cli unchanged; existing ReportController tests pass; stages: specified, planned, implemented, verified, accepted
- [x] **tested:** Unit tests for controllers, services, repositories; integration tests for full job lifecycle; SSE event stream test; stages: specified, planned, implemented, verified, accepted

## Required artifacts

- spec
- implementation-plan
- test-report
