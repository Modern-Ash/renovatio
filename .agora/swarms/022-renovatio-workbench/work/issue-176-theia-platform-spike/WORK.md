---
schema: "agora/work/v1"
id: "issue-176-theia-platform-spike"
swarm: "renovatio-workbench"
title: "Issue #176 \u00b7 Theia 0 \u00b7 Spike de plataforma y distribuci\u00f3n"
state: "verifying"
revision: 1
operational-status: "blocked"
status-reason: "Verification cannot complete in the current environment: no interactive browser instance or macOS executor is available for the required widget/menu capture and cross-platform run. The Theia CLI development graph also retains the documented critical decompress advisory pending an upstream fix or explicit security disposition."
status-by: "project:agent"
status-at: "2026-09-06T02:16:39.311858Z"
acceptance-criteria: {"prototype-build":"A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.","renovatio-widget":"A Renovatio widget is visible and a registered command opens its custom view.","platform-distribution":"Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.","extension-compatibility":"Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.","environment-security":"Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.","adr-decision":"An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.","non-production-impact":"The spike does not modify or replace the current production flow or dashboard.","verification-evidence":"A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence."}
satisfied-criteria: []
criterion-statuses: {"prototype-build":["specified","planned","implemented","verified"],"renovatio-widget":["specified","planned","implemented"],"platform-distribution":["specified","planned","implemented"],"extension-compatibility":["specified","planned","implemented","verified"],"environment-security":["specified","planned","implemented","verified"],"adr-decision":["specified","planned","implemented","verified"],"non-production-impact":["specified","planned","implemented","verified"],"verification-evidence":["specified","planned","implemented"]}
required-artifacts: ["spec","implementation-plan","prototype","architecture-decision-record","runbook","compatibility-risk-matrix","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Issue #176 · Theia 0 · Spike de plataforma y distribución

## Description

Validate Eclipse Theia as the web-first platform for Renovatio before migrating production screens. Build a minimal local-workspace prototype, evaluate browser and desktop distribution, extensions, configuration, licensing, telemetry and security constraints, without changing the production flow.

## Acceptance criteria

- [ ] **prototype-build:** A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.; stages: specified, planned, implemented, verified
- [ ] **renovatio-widget:** A Renovatio widget is visible and a registered command opens its custom view.; stages: specified, planned, implemented
- [ ] **platform-distribution:** Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.; stages: specified, planned, implemented
- [ ] **extension-compatibility:** Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.; stages: specified, planned, implemented, verified
- [ ] **environment-security:** Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.; stages: specified, planned, implemented, verified
- [ ] **adr-decision:** An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.; stages: specified, planned, implemented, verified
- [ ] **non-production-impact:** The spike does not modify or replace the current production flow or dashboard.; stages: specified, planned, implemented, verified
- [ ] **verification-evidence:** A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence.; stages: specified, planned, implemented

## Required artifacts

- spec
- implementation-plan
- prototype
- architecture-decision-record
- runbook
- compatibility-risk-matrix
- verification-report
- review-report
