---
schema: "agora/work/v1"
id: "issue-176-theia-platform-spike"
swarm: "renovatio-workbench"
title: "Issue #176 \u00b7 Theia 0 \u00b7 Spike de plataforma y distribuci\u00f3n"
state: "implementing"
revision: 1
operational-status: "active"
status-reason: null
status-by: null
status-at: null
acceptance-criteria: {"prototype-build":"A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.","renovatio-widget":"A Renovatio widget is visible and a registered command opens its custom view.","platform-distribution":"Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.","extension-compatibility":"Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.","environment-security":"Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.","adr-decision":"An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.","non-production-impact":"The spike does not modify or replace the current production flow or dashboard.","verification-evidence":"A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence."}
satisfied-criteria: []
criterion-statuses: {"prototype-build":["specified","planned"],"renovatio-widget":["specified","planned"],"platform-distribution":["specified","planned"],"extension-compatibility":["specified","planned"],"environment-security":["specified","planned"],"adr-decision":["specified","planned"],"non-production-impact":["specified","planned"],"verification-evidence":["specified","planned"]}
required-artifacts: ["spec","implementation-plan","prototype","architecture-decision-record","runbook","compatibility-risk-matrix","verification-report","review-report"]
child-work-refs: []
budget-limits: null
---

# Issue #176 · Theia 0 · Spike de plataforma y distribución

## Description

Validate Eclipse Theia as the web-first platform for Renovatio before migrating production screens. Build a minimal local-workspace prototype, evaluate browser and desktop distribution, extensions, configuration, licensing, telemetry and security constraints, without changing the production flow.

## Acceptance criteria

- [ ] **prototype-build:** A minimal Theia application with a local workspace builds cleanly in CI and starts successfully.; stages: specified, planned
- [ ] **renovatio-widget:** A Renovatio widget is visible and a registered command opens its custom view.; stages: specified, planned
- [ ] **platform-distribution:** Browser execution is confirmed on Linux and macOS, with desktop packaging evaluated as a secondary distribution.; stages: specified, planned
- [ ] **extension-compatibility:** Open VSX, extension compatibility, pinned Node version, file tree integration, menu and command behavior are validated and documented.; stages: specified, planned
- [ ] **environment-security:** Authentication, backend URL and per-environment configuration are defined; licensing, telemetry, CSP and extension limits are documented.; stages: specified, planned
- [ ] **adr-decision:** An ADR compares Theia, Code-OSS, OpenVSCode Server and Monaco, justifies the platform decision and pins the selected Theia version.; stages: specified, planned
- [ ] **non-production-impact:** The spike does not modify or replace the current production flow or dashboard.; stages: specified, planned
- [ ] **verification-evidence:** A clean build, HTTP smoke test, prototype capture and technical review are recorded as successful evidence.; stages: specified, planned

## Required artifacts

- spec
- implementation-plan
- prototype
- architecture-decision-record
- runbook
- compatibility-risk-matrix
- verification-report
- review-report
